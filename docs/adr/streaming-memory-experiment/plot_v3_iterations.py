"""
V3 iteration sweep — does footprint stay flat as cumulative throughput grows 10x?

Reads v3-heap-{32m,64m,128m}-iter10.csv (same workload repeated 10x, 1.18M rows total)
and produces v3-heap-iterations.png.
"""
import csv
import os
import matplotlib.pyplot as plt

script_dir = os.path.dirname(os.path.abspath(__file__))
configs = [
    ('32m-iter10', '32m', '#27ae60'),
    ('64m-iter10', '64m', '#2980b9'),
    ('128m-iter10', '128m', '#e67e22'),
]

fig, ax = plt.subplots(figsize=(16, 8))

stats = []
all_iter_boundaries = set()
for label, heap_str, color in configs:
    csv_path = os.path.join(script_dir, f'v3-heap-{label}.csv')
    counts, heaps, maxes, elapsed, iters = [], [], [], [], []
    with open(csv_path) as f:
        for row in csv.DictReader(f):
            counts.append(int(row['count']))
            heaps.append(int(row['heap_used_mb']))
            maxes.append(int(row['heap_max_mb']))
            elapsed.append(int(row['elapsed_ms']))
            iters.append(int(row['iteration']))
    heap_limit = maxes[0]
    ax.plot(counts, heaps, color=color, linewidth=0.8, alpha=0.8,
            label=f'-Xmx{heap_str} (range {min(heaps)}~{max(heaps)}MB, {elapsed[-1]/1000:.1f}s)')
    ax.axhline(y=heap_limit, color=color, linestyle=':', linewidth=0.7, alpha=0.4)
    # iteration boundaries: last count per iteration
    prev_iter = 1
    for c, it in zip(counts, iters):
        if it != prev_iter:
            all_iter_boundaries.add(c // 1000 * 1000)  # round
            prev_iter = it
    stats.append({
        'heap': heap_str, 'limit': heap_limit,
        'min': min(heaps), 'max': max(heaps),
        'mean': sum(heaps) / len(heaps),
        'time_s': elapsed[-1] / 1000,
        'throughput_k': counts[-1] / (elapsed[-1] / 1000) / 1000,
    })

# vertical lines at every 118122 step
for i in range(1, 10):
    ax.axvline(x=118122 * i, color='#7f8c8d', linewidth=0.4, linestyle=':', alpha=0.4)

ax.set_xlabel('Cumulative processed entries (10 iterations of the same 118k workload)', fontsize=11)
ax.set_ylabel('Heap usage (MB)', fontsize=11)
ax.set_title('Iteration sweep — same workload × 10, three heap limits\n'
             '1.18M rows total. Footprint min holds steady; only GC slack scales with -Xmx.',
             fontsize=12, pad=15)
ax.set_ylim(0, 140)
ax.set_xlim(0, 1_200_000)
ax.grid(True, alpha=0.3)
ax.set_xticks([i * 118122 for i in range(0, 11)])
ax.set_xticklabels([f'{i}x' for i in range(0, 11)])
ax.legend(loc='upper right', framealpha=0.95, fontsize=10)

table_text = f'{"heap":>5} {"min":>5} {"mean":>5} {"max":>5} {"time":>7} {"throughput":>11}\n'
for s in stats:
    table_text += f'{s["limit"]:>4}m {s["min"]:>4}MB {s["mean"]:>4.0f}MB {s["max"]:>4}MB {s["time_s"]:>5.1f}s  {s["throughput_k"]:>7.1f}k/s\n'
ax.text(0.02, 0.97, table_text.rstrip(), transform=ax.transAxes, fontsize=9,
        verticalalignment='top', family='monospace',
        bbox=dict(boxstyle='round,pad=0.5', facecolor='#ecf0f1', edgecolor='#95a5a6', alpha=0.95))

plt.tight_layout()
output_path = os.path.join(script_dir, 'v3-heap-iterations.png')
plt.savefig(output_path, dpi=140, bbox_inches='tight')
print(f'Saved: {output_path}')
for s in stats:
    print(f'  -Xmx{s["heap"]}: heap {s["min"]}~{s["max"]}MB (mean {s["mean"]:.1f}), {s["time_s"]:.1f}s, {s["throughput_k"]:.1f}k/s')
