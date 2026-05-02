"""
JVM G1GC heap pattern visualizer — v2 (10x data, 1.17M entries).

Reads gc-log-v2.txt (raw log output) and produces heap-pattern-v2.png.

Captured from CorpCodeFetchSmokeTestV2 with -Xms64m -Xmx64m -Xlog:gc.
Same zip data parsed 10 times (1,174,960 total entries) to verify steady state
beyond the original 117k single-pass experiment.
"""
import os
import re
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches

script_dir = os.path.dirname(os.path.abspath(__file__))
log_path = os.path.join(script_dir, 'gc-log-v2.txt')

with open(log_path, 'r') as f:
    log = f.read()

# parse heap data points: "processed=COUNT  heap=MMMB / 64MB"
heap_pattern = re.compile(r'processed=(\d+)\s+heap=(\d+)MB')
heap_data = [(int(m.group(1)), int(m.group(2))) for m in heap_pattern.finditer(log)]

# parse iteration boundaries: "--- iteration N done, total=COUNT ---"
iter_pattern = re.compile(r'--- iteration (\d+) done, total=(\d+) ---')
iterations = [(int(m.group(1)), int(m.group(2))) for m in iter_pattern.finditer(log)]

# parse Full GC: only one expected
full_gc_match = re.search(r'\[(\d+\.\d+)s\].*Pause Full.*?(\d+)M->(\d+)M', log)

# we approximate "where in count timeline" each GC log line happened
# by tracking count progress as we scan the log linearly
def find_count_at_position(target_line):
    """Find the most recent processed=COUNT line before target line."""
    pos = log.find(target_line)
    if pos == -1:
        return None
    snippet = log[:pos]
    matches = list(heap_pattern.finditer(snippet))
    if matches:
        return int(matches[-1].group(1))
    return 0

# locate Full GC count position
full_gc_count = None
if full_gc_match:
    full_gc_line = full_gc_match.group(0)
    full_gc_count = find_count_at_position(full_gc_line)

# evacuation failures
evac_failures = []
for m in re.finditer(r'\[(\d+\.\d+)s\].*Evacuation Failure', log):
    line = m.group(0)
    count = find_count_at_position(line)
    if count is not None:
        evac_failures.append(count)

# build chart
counts = [d[0] for d in heap_data]
heaps = [d[1] for d in heap_data]

fig, ax = plt.subplots(figsize=(18, 8))

# main heap line — thinner since there are many points
ax.plot(counts, heaps, color='#2c3e50', linewidth=0.7, zorder=2, label='Heap usage')

# fill under
ax.fill_between(counts, 0, heaps, color='#3498db', alpha=0.06)

# heap limit
ax.axhline(y=64, color='gray', linestyle='--', linewidth=0.8, alpha=0.6, label='-Xmx64m limit')

# steady state band (30 ~ 62 MB observed range)
ax.axhspan(30, 62, color='green', alpha=0.05, zorder=0, label='Steady state band (30~62MB)')

# iteration boundaries
for iter_num, total_at_end in iterations:
    ax.axvline(x=total_at_end, color='#7f8c8d', linewidth=0.5, linestyle=':', alpha=0.6, zorder=1)
    if iter_num in (1, 5, 10):
        ax.text(total_at_end, 67, f'iter {iter_num}', fontsize=8, ha='center', color='#7f8c8d')

# evacuation failures (orange dots near top)
for count in evac_failures:
    ax.scatter([count], [62.5], color='#f39c12', s=30, zorder=3, marker='v', alpha=0.7)

# Full GC (single, big red star)
if full_gc_count is not None:
    ax.scatter([full_gc_count], [63], color='#c0392b', s=200, marker='*', zorder=4,
               edgecolor='white', linewidth=1.5)
    ax.annotate('Full GC (emergency)\n62MB → 23MB, 15ms pause',
                xy=(full_gc_count, 63), xytext=(full_gc_count + 80000, 70),
                fontsize=9, color='#c0392b',
                arrowprops=dict(arrowstyle='->', color='#c0392b'),
                bbox=dict(boxstyle='round,pad=0.4', facecolor='white', edgecolor='#c0392b', alpha=0.95))

# legend pieces
legend_handles = [
    mpatches.Patch(color='#3498db', alpha=0.4, label='Heap usage'),
    mpatches.Patch(color='gray', label='-Xmx64m limit (dashed)'),
    mpatches.Patch(color='#7f8c8d', label='Iteration boundary (dotted)'),
    mpatches.Patch(color='#f39c12', label=f'Evacuation Failure ({len(evac_failures)} events)'),
    mpatches.Patch(color='#c0392b', label='Full GC (1 event — emergency)'),
    mpatches.Patch(color='green', alpha=0.3, label='Steady state band'),
]
ax.legend(handles=legend_handles, loc='lower right', framealpha=0.95, fontsize=9)

# axis
ax.set_xlabel('Processed entries (cumulative across 10 iterations)', fontsize=11)
ax.set_ylabel('Heap usage (MB)', fontsize=11)
ax.set_title('JVM G1GC Heap Pattern — v2 (10x data, steady state confirmed)\n'
             '1,174,960 companies parsed via StAX streaming under -Xmx64m, batch size 1000',
             fontsize=12, pad=15)

ax.set_ylim(0, 75)
ax.set_xlim(0, 1_200_000)
ax.grid(True, alpha=0.3)

# x-axis formatting (k or M)
ax.set_xticks([i * 100_000 for i in range(0, 13)])
ax.set_xticklabels([f'{i*100}k' if i < 10 else f'{i/10:.1f}M' for i in range(0, 13)])

# observation summary in corner
summary = (f'{len(heap_data)} heap samples\n'
           f'{len(iterations)} iterations\n'
           f'{len(evac_failures)} Evacuation Failures\n'
           f'1 Full GC (emergency)\n'
           f'Total time: ~4.4s\n'
           f'Throughput: ~270k/sec')
ax.text(0.02, 0.97, summary, transform=ax.transAxes, fontsize=9,
        verticalalignment='top', family='monospace',
        bbox=dict(boxstyle='round,pad=0.5', facecolor='#ecf0f1', edgecolor='#95a5a6', alpha=0.95))

plt.tight_layout()
output_path = os.path.join(script_dir, 'heap-pattern-v2.png')
plt.savefig(output_path, dpi=140, bbox_inches='tight')
print(f'Saved: {output_path}')
print(f'Heap samples: {len(heap_data)}')
print(f'Iterations: {len(iterations)}')
print(f'Evacuation failures: {len(evac_failures)}')
print(f'Full GC: {1 if full_gc_count else 0}')
