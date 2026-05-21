"""
V3 heap-size comparison — does footprint scale with the heap limit?

Reads v3-heap-{32m,64m,128m,256m}.csv and produces v3-heap-compare.png.
"""
import csv
import os
import matplotlib.pyplot as plt

script_dir = os.path.dirname(os.path.abspath(__file__))
labels = ['32m', '64m', '128m', '256m']
colors = ['#27ae60', '#2980b9', '#e67e22', '#c0392b']

fig, ax = plt.subplots(figsize=(15, 8))

stats = []
for label, color in zip(labels, colors):
    csv_path = os.path.join(script_dir, f'v3-heap-{label}.csv')
    counts, heaps, maxes, elapsed = [], [], [], []
    with open(csv_path) as f:
        for row in csv.DictReader(f):
            counts.append(int(row['count']))
            heaps.append(int(row['heap_used_mb']))
            maxes.append(int(row['heap_max_mb']))
            elapsed.append(int(row['elapsed_ms']))
    heap_limit = maxes[0]
    ax.plot(counts, heaps, color=color, linewidth=1.0, alpha=0.85,
            label=f'-Xmx{label} (range {min(heaps)}~{max(heaps)}MB, {elapsed[-1]/1000:.2f}s)')
    ax.axhline(y=heap_limit, color=color, linestyle=':', linewidth=0.8, alpha=0.5)
    stats.append({
        'label': label, 'limit': heap_limit,
        'min': min(heaps), 'max': max(heaps),
        'mean': sum(heaps) / len(heaps),
        'time_s': elapsed[-1] / 1000,
        'throughput_k': counts[-1] / (elapsed[-1] / 1000) / 1000,
    })

ax.set_xlabel('Processed entries', fontsize=11)
ax.set_ylabel('Heap usage (MB)', fontsize=11)
ax.set_title('Heap footprint vs. -Xmx limit — same workload (118,122 rows), 4 limits\n'
             'If footprint were proportional to limit, lines would diverge. They don\'t — '
             'minimum footprint stays flat; only GC slack grows.',
             fontsize=12, pad=15)
ax.set_ylim(0, 280)
ax.set_xlim(0, 125_000)
ax.grid(True, alpha=0.3)
ax.set_xticks([i * 20_000 for i in range(0, 7)])
ax.set_xticklabels([f'{i*20}k' for i in range(0, 7)])

ax.legend(loc='upper right', framealpha=0.95, fontsize=9)

# stats table
table_text = f'{"limit":>6} {"min":>5} {"mean":>5} {"max":>5} {"time":>7} {"throughput":>11}\n'
for s in stats:
    table_text += f'{s["limit"]:>5}m {s["min"]:>4}MB {s["mean"]:>4.0f}MB {s["max"]:>4}MB {s["time_s"]:>5.2f}s  {s["throughput_k"]:>7.1f}k/s\n'
ax.text(0.02, 0.97, table_text.rstrip(), transform=ax.transAxes, fontsize=9,
        verticalalignment='top', family='monospace',
        bbox=dict(boxstyle='round,pad=0.5', facecolor='#ecf0f1', edgecolor='#95a5a6', alpha=0.95))

plt.tight_layout()
output_path = os.path.join(script_dir, 'v3-heap-compare.png')
plt.savefig(output_path, dpi=140, bbox_inches='tight')
print(f'Saved: {output_path}')
for s in stats:
    print(f'  -Xmx{s["label"]}: heap {s["min"]}~{s["max"]}MB (mean {s["mean"]:.1f}), {s["time_s"]:.2f}s')
