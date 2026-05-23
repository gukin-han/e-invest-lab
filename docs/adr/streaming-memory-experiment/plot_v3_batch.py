"""
V3 batch-size sweep — how does the JDBC flush interval move the footprint?

Reads v3-heap-64m-batch{100,500,1000,5000,10000}.csv (fixed -Xmx64m, same workload)
and produces v3-heap-batch.png + v3-heap-batch-summary.png.

- v3-heap-batch.png: overlay of three batch sizes (100/1000/10000) to keep lines readable.
- v3-heap-batch-summary.png: 2-panel summary using ALL five batch sizes.
  Right panel shows BOTH min and max so the floor-rises-with-batch claim is visible.
"""
import csv
import os
import matplotlib.pyplot as plt

script_dir = os.path.dirname(os.path.abspath(__file__))
all_batch_sizes = [100, 500, 1000, 5000, 10000]
overlay_batches = [100, 1000, 10000]
colors = {
    100: '#27ae60',
    500: '#16a085',
    1000: '#2980b9',
    5000: '#e67e22',
    10000: '#c0392b',
}

# load everything once
data = {}
stats = []
for batch in all_batch_sizes:
    csv_path = os.path.join(script_dir, f'v3-heap-64m-batch{batch}.csv')
    counts, heaps, elapsed = [], [], []
    with open(csv_path) as f:
        for row in csv.DictReader(f):
            counts.append(int(row['count']))
            heaps.append(int(row['heap_used_mb']))
            elapsed.append(int(row['elapsed_ms']))
    data[batch] = (counts, heaps, elapsed)
    stats.append({
        'batch': batch,
        'flushes': len(counts),
        'min': min(heaps),
        'max': max(heaps),
        'mean': sum(heaps) / len(heaps),
        'time_s': elapsed[-1] / 1000,
        'throughput_k': counts[-1] / (elapsed[-1] / 1000) / 1000,
    })

# === plot 1: overlay (3 batches only) ===
fig, ax = plt.subplots(figsize=(16, 8))

for batch in overlay_batches:
    counts, heaps, elapsed = data[batch]
    ax.plot(counts, heaps, color=colors[batch], linewidth=1.0, alpha=0.85,
            label=f'batch={batch} ({len(counts)} flushes, '
                  f'min {min(heaps)}MB / max {max(heaps)}MB, {elapsed[-1]/1000:.2f}s)')

ax.axhline(y=64, color='gray', linestyle='--', linewidth=0.8, alpha=0.7, label='-Xmx64m limit')

ax.set_xlabel('Processed entries', fontsize=11)
ax.set_ylabel('Heap usage (MB)', fontsize=11)
ax.set_title('Batch-size sweep at fixed -Xmx64m — showing 100 / 1000 / 10000\n'
             'Larger batch → fewer flushes → both floor and ceiling shift up.',
             fontsize=12, pad=15)
ax.set_ylim(0, 70)
ax.set_xlim(0, 125_000)
ax.grid(True, alpha=0.3)
ax.set_xticks([i * 20_000 for i in range(0, 7)])
ax.set_xticklabels([f'{i*20}k' for i in range(0, 7)])
ax.legend(loc='lower right', framealpha=0.95, fontsize=10)

# stats table (all 5 batches — still informative)
table_text = f'{"batch":>6} {"flushes":>8} {"min":>5} {"max":>5} {"time":>7} {"throughput":>11}\n'
for s in stats:
    table_text += (f'{s["batch"]:>6} {s["flushes"]:>8} {s["min"]:>4}MB {s["max"]:>4}MB '
                   f'{s["time_s"]:>5.2f}s  {s["throughput_k"]:>7.1f}k/s\n')
ax.text(0.02, 0.97, table_text.rstrip(), transform=ax.transAxes, fontsize=9,
        verticalalignment='top', family='monospace',
        bbox=dict(boxstyle='round,pad=0.5', facecolor='#ecf0f1', edgecolor='#95a5a6', alpha=0.95))

plt.tight_layout()
plot_path = os.path.join(script_dir, 'v3-heap-batch.png')
plt.savefig(plot_path, dpi=140, bbox_inches='tight')
print(f'Saved: {plot_path}')

# === plot 2: summary using 5 trials per batch — mean ± std ===
# Load trial data: v3-heap-64m-batch{B}-t{1..5}.csv
trial_data = {b: {'mins': [], 'maxes': [], 'throughputs': []} for b in all_batch_sizes}
for batch in all_batch_sizes:
    for t in range(1, 6):
        csv_path = os.path.join(script_dir, f'v3-heap-64m-batch{batch}-t{t}.csv')
        counts, heaps, elapsed = [], [], []
        with open(csv_path) as f:
            for row in csv.DictReader(f):
                counts.append(int(row['count']))
                heaps.append(int(row['heap_used_mb']))
                elapsed.append(int(row['elapsed_ms']))
        trial_data[batch]['mins'].append(min(heaps))
        trial_data[batch]['maxes'].append(max(heaps))
        trial_data[batch]['throughputs'].append(counts[-1] / (elapsed[-1] / 1000) / 1000)


def mean(lst):
    return sum(lst) / len(lst)


def stdev(lst):
    m = mean(lst)
    return (sum((x - m) ** 2 for x in lst) / len(lst)) ** 0.5


batches = all_batch_sizes
mean_mins = [mean(trial_data[b]['mins']) for b in batches]
std_mins = [stdev(trial_data[b]['mins']) for b in batches]
mean_maxes = [mean(trial_data[b]['maxes']) for b in batches]
std_maxes = [stdev(trial_data[b]['maxes']) for b in batches]
mean_tps = [mean(trial_data[b]['throughputs']) for b in batches]
std_tps = [stdev(trial_data[b]['throughputs']) for b in batches]

fig2 = plt.figure(figsize=(14, 11))
gs = fig2.add_gridspec(2, 2, height_ratios=[1, 1.1], hspace=0.35)
ax1 = fig2.add_subplot(gs[0, 0])
ax2 = fig2.add_subplot(gs[0, 1])
ax3 = fig2.add_subplot(gs[1, :])


def scatter_trials(ax, xs, ys_per_batch, color):
    for x, ys in zip(xs, ys_per_batch):
        for y in ys:
            ax.scatter(x, y, color=color, alpha=0.35, s=35, zorder=3)


# ax1: throughput (log x) — scatter + mean
scatter_trials(ax1, batches, [trial_data[b]['throughputs'] for b in batches], '#2980b9')
ax1.plot(batches, mean_tps, marker='o', markersize=9, color='#2980b9', linewidth=2,
         label='throughput — mean of 5 trials', zorder=4)
for x, m, s in zip(batches, mean_tps, std_tps):
    ax1.annotate(f'{m:.1f}±{s:.1f}', xy=(x, m), xytext=(7, 8), textcoords='offset points',
                 fontsize=9, color='#2980b9')
ax1.set_xscale('log')
ax1.set_xlabel('Batch size (log scale)', fontsize=11)
ax1.set_ylabel('Throughput (rows/sec, thousands)', fontsize=11)
ax1.set_title('Throughput vs batch size — 5 trials each', fontsize=12)
ax1.grid(True, alpha=0.3, which='both')
ax1.legend(loc='lower right', fontsize=10)


# ax2: heap min/max (log x) — scatter + mean lines + fill
scatter_trials(ax2, batches, [trial_data[b]['mins'] for b in batches], '#27ae60')
scatter_trials(ax2, batches, [trial_data[b]['maxes'] for b in batches], '#c0392b')
ax2.fill_between(batches, mean_mins, mean_maxes, color='#7f8c8d', alpha=0.15,
                 label='heap band (mean min ↔ mean max)', zorder=1)
ax2.plot(batches, mean_maxes, marker='o', markersize=9, color='#c0392b', linewidth=2,
         label='max — mean of 5 trials', zorder=4)
ax2.plot(batches, mean_mins, marker='s', markersize=9, color='#27ae60', linewidth=2,
         label='min — mean of 5 trials', zorder=4)
for x, m, s in zip(batches, mean_maxes, std_maxes):
    ax2.annotate(f'{m:.1f}±{s:.1f}', xy=(x, m), xytext=(7, 8), textcoords='offset points',
                 fontsize=9, color='#c0392b')
for x, m, s in zip(batches, mean_mins, std_mins):
    ax2.annotate(f'{m:.1f}±{s:.1f}', xy=(x, m), xytext=(7, -16), textcoords='offset points',
                 fontsize=9, color='#27ae60')
ax2.axhline(y=64, color='gray', linestyle='--', linewidth=0.8, alpha=0.7, label='-Xmx64m limit')
ax2.set_xscale('log')
ax2.set_xlabel('Batch size (log scale)', fontsize=11)
ax2.set_ylabel('Heap usage (MB)', fontsize=11)
ax2.set_title('Heap min/max vs batch size — floor rises with batch', fontsize=12)
ax2.set_ylim(0, 70)
ax2.grid(True, alpha=0.3, which='both')
ax2.legend(loc='lower right', fontsize=9)


# ax3: heap min/max (linear x) — same data, exposes the 1000→5000 jump
scatter_trials(ax3, batches, [trial_data[b]['mins'] for b in batches], '#27ae60')
scatter_trials(ax3, batches, [trial_data[b]['maxes'] for b in batches], '#c0392b')
ax3.fill_between(batches, mean_mins, mean_maxes, color='#7f8c8d', alpha=0.15, zorder=1)
ax3.plot(batches, mean_maxes, marker='o', markersize=10, color='#c0392b', linewidth=2,
         label='max — mean of 5 trials', zorder=4)
ax3.plot(batches, mean_mins, marker='s', markersize=10, color='#27ae60', linewidth=2,
         label='min — mean of 5 trials', zorder=4)
for x, m, s in zip(batches, mean_maxes, std_maxes):
    ax3.annotate(f'{m:.1f}±{s:.1f}', xy=(x, m), xytext=(8, 10), textcoords='offset points',
                 fontsize=10, color='#c0392b')
for x, m, s in zip(batches, mean_mins, std_mins):
    ax3.annotate(f'{m:.1f}±{s:.1f}', xy=(x, m), xytext=(8, -18), textcoords='offset points',
                 fontsize=10, color='#27ae60')
ax3.axhline(y=64, color='gray', linestyle='--', linewidth=0.8, alpha=0.7, label='-Xmx64m limit')
ax3.set_xlabel('Batch size (LINEAR scale)', fontsize=11)
ax3.set_ylabel('Heap usage (MB)', fontsize=11)
ax3.set_title('Same data, linear x — the 1000 → 5000 jump and band collapse become obvious',
              fontsize=12)
ax3.set_ylim(0, 70)
ax3.set_xlim(-200, 10500)
ax3.grid(True, alpha=0.3)
ax3.legend(loc='lower right', fontsize=10)

plt.suptitle('Batch-size sweep summary — fixed -Xmx64m, 5 trials per batch size (dots = trials, lines = means)',
             fontsize=13)
summary_path = os.path.join(script_dir, 'v3-heap-batch-summary.png')
plt.savefig(summary_path, dpi=140, bbox_inches='tight')
print(f'Saved: {summary_path}')

print()
print(f'{"batch":>6}  {"min mean±std":>14}  {"max mean±std":>14}  {"throughput":>14}')
for b in batches:
    print(f'{b:>6}  '
          f'{mean(trial_data[b]["mins"]):>6.1f}±{stdev(trial_data[b]["mins"]):.1f}MB  '
          f'{mean(trial_data[b]["maxes"]):>6.1f}±{stdev(trial_data[b]["maxes"]):.1f}MB  '
          f'{mean(trial_data[b]["throughputs"]):>5.1f}±{stdev(trial_data[b]["throughputs"]):.1f}k/s')

for s in stats:
    print(f'  batch={s["batch"]:>5}: heap {s["min"]}~{s["max"]}MB, {s["time_s"]:.2f}s, {s["throughput_k"]:.1f}k/s')
