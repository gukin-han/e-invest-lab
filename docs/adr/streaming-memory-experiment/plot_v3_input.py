"""
V3 input-size sweep — does footprint scale with the single-shot input size?

Reads v3-heap-64m-input{1,10,100}x.csv (118k / 1.18M / 11.8M rows in one streaming pass)
and produces v3-heap-input.png + v3-heap-input-summary.png.
"""
import csv
import os
import matplotlib.pyplot as plt

script_dir = os.path.dirname(os.path.abspath(__file__))
multipliers = [1, 10, 100]
colors = ['#27ae60', '#2980b9', '#c0392b']
labels = ['1x (118k rows, 30MB XML)', '10x (1.18M rows, 300MB XML)', '100x (11.8M rows, 3GB XML)']

fig, ax = plt.subplots(figsize=(16, 8))

stats = []
for mul, color, label in zip(multipliers, colors, labels):
    csv_path = os.path.join(script_dir, f'v3-heap-64m-input{mul}x.csv')
    counts, heaps, elapsed = [], [], []
    with open(csv_path) as f:
        for row in csv.DictReader(f):
            counts.append(int(row['count']))
            heaps.append(int(row['heap_used_mb']))
            elapsed.append(int(row['elapsed_ms']))
    # downsample 100x for plotting (11k points → 1k)
    if len(counts) > 2000:
        step = len(counts) // 1000
        counts = counts[::step]
        heaps = heaps[::step]
        elapsed = elapsed[::step]
    ax.plot(counts, heaps, color=color, linewidth=0.6, alpha=0.75, label=label)
    stats.append({
        'mul': mul,
        'rows': mul * 118122,
        'min': min(heaps),
        'max': max(heaps),
        'mean': sum(heaps) / len(heaps),
        'time_s': elapsed[-1] / 1000,
        'throughput_k': (mul * 118122) / (elapsed[-1] / 1000) / 1000,
    })

ax.axhline(y=64, color='gray', linestyle='--', linewidth=0.8, alpha=0.7, label='-Xmx64m limit')

ax.set_xlabel('Processed entries (cumulative within a single streaming pass)', fontsize=11)
ax.set_ylabel('Heap usage (MB)', fontsize=11)
ax.set_title('Input-size sweep — single-pass 30MB / 300MB / 3GB XML, fixed -Xmx64m, batch=1000\n'
             'The 3GB case fits in the same heap as the 30MB case. Footprint is independent of input size.',
             fontsize=12, pad=15)
ax.set_ylim(0, 75)
ax.set_xlim(0, 12_500_000)
ax.grid(True, alpha=0.3)
ax.set_xticks([i * 2_000_000 for i in range(0, 7)])
ax.set_xticklabels([f'{i*2}M' if i > 0 else '0' for i in range(0, 7)])
ax.legend(loc='upper right', framealpha=0.95, fontsize=10)

table_text = f'{"size":>5} {"rows":>12} {"min":>5} {"max":>5} {"time":>8} {"throughput":>11}\n'
for s in stats:
    table_text += (f'{s["mul"]:>4}x {s["rows"]:>11,} {s["min"]:>4}MB {s["max"]:>4}MB '
                   f'{s["time_s"]:>6.1f}s  {s["throughput_k"]:>7.1f}k/s\n')
ax.text(0.02, 0.97, table_text.rstrip(), transform=ax.transAxes, fontsize=9,
        verticalalignment='top', family='monospace',
        bbox=dict(boxstyle='round,pad=0.5', facecolor='#ecf0f1', edgecolor='#95a5a6', alpha=0.95))

plt.tight_layout()
plot_path = os.path.join(script_dir, 'v3-heap-input.png')
plt.savefig(plot_path, dpi=140, bbox_inches='tight')
print(f'Saved: {plot_path}')

# summary: peak heap stays flat as input grows
fig2, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5.5))

xs = [s['rows'] for s in stats]
peak_heaps = [s['max'] for s in stats]
times = [s['time_s'] for s in stats]

ax1.plot(xs, peak_heaps, marker='o', markersize=10, color='#c0392b', linewidth=2)
for s in stats:
    ax1.annotate(f'{s["max"]}MB', xy=(s['rows'], s['max']),
                 xytext=(8, 8), textcoords='offset points', fontsize=10)
ax1.axhline(y=64, color='gray', linestyle='--', linewidth=0.8, alpha=0.7, label='-Xmx64m limit')
ax1.set_xscale('log')
ax1.set_xlabel('Input size (rows, log scale)', fontsize=11)
ax1.set_ylabel('Peak heap usage (MB)', fontsize=11)
ax1.set_title('Peak heap stays flat as input grows 100x', fontsize=12)
ax1.set_ylim(0, 75)
ax1.grid(True, alpha=0.3, which='both')
ax1.legend(loc='lower right', fontsize=10)

ax2.plot(xs, times, marker='o', markersize=10, color='#2980b9', linewidth=2, label='measured')
# linear reference line through 1x point
linear_y = [times[0] * x / xs[0] for x in xs]
ax2.plot(xs, linear_y, linestyle=':', color='gray', linewidth=1.5, label='perfect linear (N · t₁)')
for s, t in zip(stats, times):
    ax2.annotate(f'{t:.1f}s', xy=(s['rows'], t),
                 xytext=(8, 8), textcoords='offset points', fontsize=10)
ax2.set_xscale('log')
ax2.set_yscale('log')
ax2.set_xlabel('Input size (rows, log scale)', fontsize=11)
ax2.set_ylabel('Processing time (seconds, log scale)', fontsize=11)
ax2.set_title('Time scales linearly with input', fontsize=12)
ax2.grid(True, alpha=0.3, which='both')
ax2.legend(loc='lower right', fontsize=10)

plt.suptitle('Input-size sweep summary — single-pass, fixed -Xmx64m', fontsize=13)
plt.tight_layout()
summary_path = os.path.join(script_dir, 'v3-heap-input-summary.png')
plt.savefig(summary_path, dpi=140, bbox_inches='tight')
print(f'Saved: {summary_path}')

for s in stats:
    print(f'  {s["mul"]:>3}x ({s["rows"]:>11,} rows): heap {s["min"]}~{s["max"]}MB, {s["time_s"]:.1f}s, {s["throughput_k"]:.1f}k/s')
