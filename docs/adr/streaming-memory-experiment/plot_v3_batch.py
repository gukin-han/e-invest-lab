"""
V3 batch-size sweep — how does the JDBC flush interval move the footprint?

Reads v3-heap-64m-batch{100,500,1000,5000,10000}.csv (fixed -Xmx64m, same workload)
and produces v3-heap-batch.png + v3-heap-batch-summary.png.
"""
import csv
import os
import matplotlib.pyplot as plt

script_dir = os.path.dirname(os.path.abspath(__file__))
batch_sizes = [100, 500, 1000, 5000, 10000]
colors = ['#27ae60', '#2980b9', '#7f8c8d', '#e67e22', '#c0392b']

# heap-pattern overlay
fig, ax = plt.subplots(figsize=(16, 8))

stats = []
for batch, color in zip(batch_sizes, colors):
    csv_path = os.path.join(script_dir, f'v3-heap-64m-batch{batch}.csv')
    counts, heaps, elapsed = [], [], []
    with open(csv_path) as f:
        for row in csv.DictReader(f):
            counts.append(int(row['count']))
            heaps.append(int(row['heap_used_mb']))
            elapsed.append(int(row['elapsed_ms']))
    ax.plot(counts, heaps, color=color, linewidth=0.8, alpha=0.8,
            label=f'batch={batch} ({len(counts)} flushes, '
                  f'{min(heaps)}~{max(heaps)}MB, {elapsed[-1]/1000:.2f}s)')
    stats.append({
        'batch': batch, 'flushes': len(counts),
        'min': min(heaps), 'max': max(heaps),
        'mean': sum(heaps) / len(heaps),
        'time_s': elapsed[-1] / 1000,
        'throughput_k': counts[-1] / (elapsed[-1] / 1000) / 1000,
    })

ax.axhline(y=64, color='gray', linestyle='--', linewidth=0.8, alpha=0.7, label='-Xmx64m limit')

ax.set_xlabel('Processed entries', fontsize=11)
ax.set_ylabel('Heap usage (MB)', fontsize=11)
ax.set_title('Batch-size sweep at fixed -Xmx64m — 100 / 500 / 1000 / 5000 / 10000\n'
             'Larger batch → fewer flushes → bigger sawtooth amplitude but better throughput.',
             fontsize=12, pad=15)
ax.set_ylim(0, 70)
ax.set_xlim(0, 125_000)
ax.grid(True, alpha=0.3)
ax.set_xticks([i * 20_000 for i in range(0, 7)])
ax.set_xticklabels([f'{i*20}k' for i in range(0, 7)])
ax.legend(loc='lower right', framealpha=0.95, fontsize=9)

table_text = f'{"batch":>6} {"flushes":>8} {"min":>5} {"max":>5} {"time":>7} {"throughput":>11}\n'
for s in stats:
    table_text += f'{s["batch"]:>6} {s["flushes"]:>8} {s["min"]:>4}MB {s["max"]:>4}MB {s["time_s"]:>5.2f}s  {s["throughput_k"]:>7.1f}k/s\n'
ax.text(0.02, 0.97, table_text.rstrip(), transform=ax.transAxes, fontsize=9,
        verticalalignment='top', family='monospace',
        bbox=dict(boxstyle='round,pad=0.5', facecolor='#ecf0f1', edgecolor='#95a5a6', alpha=0.95))

plt.tight_layout()
plot_path = os.path.join(script_dir, 'v3-heap-batch.png')
plt.savefig(plot_path, dpi=140, bbox_inches='tight')
print(f'Saved: {plot_path}')

# summary: throughput vs batch size, log-x
fig2, (ax1, ax2) = plt.subplots(1, 2, figsize=(14, 5.5))

batches = [s['batch'] for s in stats]
throughputs = [s['throughput_k'] for s in stats]
heaps_max = [s['max'] for s in stats]

ax1.plot(batches, throughputs, marker='o', markersize=8, color='#2980b9', linewidth=1.8)
for x, y in zip(batches, throughputs):
    ax1.annotate(f'{y:.0f}k/s', xy=(x, y), xytext=(5, 8), textcoords='offset points', fontsize=9)
ax1.set_xscale('log')
ax1.set_xlabel('Batch size (log scale)', fontsize=11)
ax1.set_ylabel('Throughput (rows/sec, thousands)', fontsize=11)
ax1.set_title('Throughput vs batch size', fontsize=12)
ax1.grid(True, alpha=0.3, which='both')

ax2.plot(batches, heaps_max, marker='o', markersize=8, color='#c0392b', linewidth=1.8, label='heap max')
for x, y in zip(batches, heaps_max):
    ax2.annotate(f'{y}MB', xy=(x, y), xytext=(5, 8), textcoords='offset points', fontsize=9)
ax2.axhline(y=64, color='gray', linestyle='--', linewidth=0.8, alpha=0.7, label='-Xmx64m limit')
ax2.set_xscale('log')
ax2.set_xlabel('Batch size (log scale)', fontsize=11)
ax2.set_ylabel('Peak heap usage (MB)', fontsize=11)
ax2.set_title('Peak heap vs batch size', fontsize=12)
ax2.set_ylim(0, 70)
ax2.grid(True, alpha=0.3, which='both')
ax2.legend(loc='lower right', fontsize=10)

plt.suptitle('Batch-size sweep summary — fixed -Xmx64m, same 118k workload',
             fontsize=13)
plt.tight_layout()
summary_path = os.path.join(script_dir, 'v3-heap-batch-summary.png')
plt.savefig(summary_path, dpi=140, bbox_inches='tight')
print(f'Saved: {summary_path}')

for s in stats:
    print(f'  batch={s["batch"]:>5}: heap {s["min"]}~{s["max"]}MB, {s["time_s"]:.2f}s, {s["throughput_k"]:.1f}k/s')
