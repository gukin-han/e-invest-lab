"""
V3 single-case heap pattern — StAX + JdbcTemplate.batchUpdate end-to-end.

Reads v3-heap-32m.csv (tightest heap limit) and produces v3-heap-32m.png.
Captured from CorpCodeFetchSmokeTestV3 with -Xms32m -Xmx32m.
"""
import csv
import os
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches

script_dir = os.path.dirname(os.path.abspath(__file__))
csv_path = os.path.join(script_dir, 'v3-heap-32m.csv')

counts, heaps, maxes, elapsed = [], [], [], []
with open(csv_path) as f:
    reader = csv.DictReader(f)
    for row in reader:
        counts.append(int(row['count']))
        heaps.append(int(row['heap_used_mb']))
        maxes.append(int(row['heap_max_mb']))
        elapsed.append(int(row['elapsed_ms']))

heap_limit = maxes[0]
total_time_s = elapsed[-1] / 1000

fig, ax = plt.subplots(figsize=(14, 7))

ax.plot(counts, heaps, color='#2c3e50', linewidth=1.0, zorder=2, marker='o', markersize=3)
ax.fill_between(counts, 0, heaps, color='#3498db', alpha=0.08)

ax.axhline(y=heap_limit, color='gray', linestyle='--', linewidth=0.8, alpha=0.7,
           label=f'-Xmx{heap_limit}m limit')

hmin, hmax = min(heaps), max(heaps)
ax.axhspan(hmin, hmax, color='green', alpha=0.06, zorder=0,
           label=f'Observed band ({hmin}~{hmax}MB)')

ax.set_xlabel('Processed entries', fontsize=11)
ax.set_ylabel('Heap usage (MB)', fontsize=11)
ax.set_title('V3 end-to-end (StAX + JdbcTemplate.batchUpdate) — heap pattern under -Xmx32m\n'
             '118,122 companies parsed and upserted to MySQL, batch size 1000',
             fontsize=12, pad=15)

ax.set_ylim(0, heap_limit + 4)
ax.set_xlim(0, max(counts) * 1.02)
ax.grid(True, alpha=0.3)

ax.set_xticks([i * 20_000 for i in range(0, 7)])
ax.set_xticklabels([f'{i*20}k' for i in range(0, 7)])

legend_handles = [
    mpatches.Patch(color='#3498db', alpha=0.5, label=f'Heap usage ({hmin}~{hmax}MB band)'),
    mpatches.Patch(color='gray', label=f'-Xmx{heap_limit}m limit'),
]
ax.legend(handles=legend_handles, loc='lower right', framealpha=0.95, fontsize=10)

summary = (f'{len(counts)} batch flushes\n'
           f'Total: {counts[-1]:,} rows\n'
           f'Time: {total_time_s:.2f}s\n'
           f'Throughput: ~{counts[-1] / total_time_s / 1000:.0f}k/sec\n'
           f'Heap min/max: {hmin}/{hmax} MB')
ax.text(0.02, 0.97, summary, transform=ax.transAxes, fontsize=9,
        verticalalignment='top', family='monospace',
        bbox=dict(boxstyle='round,pad=0.5', facecolor='#ecf0f1', edgecolor='#95a5a6', alpha=0.95))

plt.tight_layout()
output_path = os.path.join(script_dir, 'v3-heap-32m.png')
plt.savefig(output_path, dpi=140, bbox_inches='tight')
print(f'Saved: {output_path}')
print(f'Samples: {len(counts)}, heap band: {hmin}~{hmax}MB, time: {total_time_s:.2f}s')
