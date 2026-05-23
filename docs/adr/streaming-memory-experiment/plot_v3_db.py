"""
V3 DB on/off comparison.

5 trials each at -Xmx64m, batch=1000, input=1x.
Only difference: jdbc.batchUpdate() called or skipped.

Reads v3-heap-64m-db{on,off}-t{1..5}.csv and produces v3-heap-db.png.
"""
import csv
import os
import matplotlib.pyplot as plt

script_dir = os.path.dirname(os.path.abspath(__file__))


def load(label):
    """Returns list of (counts, heaps, elapsed) per trial."""
    trials = []
    for t in range(1, 6):
        path = os.path.join(script_dir, f'v3-heap-64m-{label}-t{t}.csv')
        counts, heaps, elapsed = [], [], []
        with open(path) as f:
            for row in csv.DictReader(f):
                counts.append(int(row['count']))
                heaps.append(int(row['heap_used_mb']))
                elapsed.append(int(row['elapsed_ms']))
        trials.append((counts, heaps, elapsed))
    return trials


def mean(lst):
    return sum(lst) / len(lst)


def stdev(lst):
    m = mean(lst)
    return (sum((x - m) ** 2 for x in lst) / len(lst)) ** 0.5


dbon_trials = load('dbon')
dboff_trials = load('dboff')

dbon_mins = [min(h) for _, h, _ in dbon_trials]
dbon_maxes = [max(h) for _, h, _ in dbon_trials]
dboff_mins = [min(h) for _, h, _ in dboff_trials]
dboff_maxes = [max(h) for _, h, _ in dboff_trials]

dbon_times = [e[-1] / 1000 for _, _, e in dbon_trials]
dboff_times = [e[-1] / 1000 for _, _, e in dboff_trials]

# === plot ===
fig = plt.figure(figsize=(15, 10))
gs = fig.add_gridspec(2, 2, height_ratios=[1, 1], hspace=0.32, wspace=0.25)
ax1 = fig.add_subplot(gs[0, :])      # overlay (one trial each)
ax2 = fig.add_subplot(gs[1, 0])       # min comparison
ax3 = fig.add_subplot(gs[1, 1])       # max comparison

# top — overlay one representative trial each (t1)
ax1.plot(dbon_trials[0][0], dbon_trials[0][1], color='#c0392b', linewidth=1.0, alpha=0.85,
         label=f'DB on (batchUpdate) — t1: min {min(dbon_trials[0][1])}MB / max {max(dbon_trials[0][1])}MB')
ax1.plot(dboff_trials[0][0], dboff_trials[0][1], color='#27ae60', linewidth=1.0, alpha=0.85,
         label=f'DB off (skip batchUpdate) — t1: min {min(dboff_trials[0][1])}MB / max {max(dboff_trials[0][1])}MB')
ax1.axhline(y=64, color='gray', linestyle='--', linewidth=0.8, alpha=0.7, label='-Xmx64m limit')
ax1.set_xlabel('Processed entries', fontsize=11)
ax1.set_ylabel('Heap usage (MB)', fontsize=11)
ax1.set_title('Heap pattern with vs without DB writes — same code, only batchUpdate toggled',
              fontsize=12)
ax1.set_ylim(0, 70)
ax1.set_xlim(0, 125_000)
ax1.grid(True, alpha=0.3)
ax1.legend(loc='upper right', fontsize=10)

# bottom-left — heap min comparison
x_labels = ['DB off\n(skip batchUpdate)', 'DB on\n(JdbcTemplate.batchUpdate)']
xs = [0, 1]
ax2.scatter([0] * 5, dboff_mins, color='#27ae60', alpha=0.5, s=80, zorder=3)
ax2.scatter([1] * 5, dbon_mins, color='#c0392b', alpha=0.5, s=80, zorder=3)
mean_dboff_min = mean(dboff_mins)
mean_dbon_min = mean(dbon_mins)
ax2.plot([0, 1], [mean_dboff_min, mean_dbon_min], marker='s', markersize=14,
         color='#2c3e50', linewidth=2, zorder=4)
ax2.annotate(f'{mean_dboff_min:.1f}±{stdev(dboff_mins):.1f}MB',
             xy=(0, mean_dboff_min), xytext=(-90, 0), textcoords='offset points',
             fontsize=11, color='#27ae60', va='center')
ax2.annotate(f'{mean_dbon_min:.1f}±{stdev(dbon_mins):.1f}MB',
             xy=(1, mean_dbon_min), xytext=(10, 0), textcoords='offset points',
             fontsize=11, color='#c0392b', va='center')
delta_min = mean_dbon_min - mean_dboff_min
ax2.annotate(f'Δ = +{delta_min:.1f}MB\n(DB adds this much\nto the floor)',
             xy=(0.5, (mean_dboff_min + mean_dbon_min) / 2),
             xytext=(0.5, 15), textcoords='data',
             fontsize=12, color='#2c3e50', ha='center', weight='bold',
             bbox=dict(boxstyle='round,pad=0.5', facecolor='#fff3cd', edgecolor='#856404'))
ax2.set_xticks([0, 1])
ax2.set_xticklabels(x_labels, fontsize=10)
ax2.set_xlim(-0.5, 1.5)
ax2.set_ylim(0, 50)
ax2.set_ylabel('Heap floor (MB)', fontsize=11)
ax2.set_title('Floor (min) — does DB raise it?', fontsize=12)
ax2.grid(True, alpha=0.3, axis='y')

# bottom-right — heap max comparison
ax3.scatter([0] * 5, dboff_maxes, color='#27ae60', alpha=0.5, s=80, zorder=3)
ax3.scatter([1] * 5, dbon_maxes, color='#c0392b', alpha=0.5, s=80, zorder=3)
mean_dboff_max = mean(dboff_maxes)
mean_dbon_max = mean(dbon_maxes)
ax3.plot([0, 1], [mean_dboff_max, mean_dbon_max], marker='o', markersize=14,
         color='#2c3e50', linewidth=2, zorder=4)
ax3.annotate(f'{mean_dboff_max:.1f}±{stdev(dboff_maxes):.1f}MB',
             xy=(0, mean_dboff_max), xytext=(-90, 0), textcoords='offset points',
             fontsize=11, color='#27ae60', va='center')
ax3.annotate(f'{mean_dbon_max:.1f}±{stdev(dbon_maxes):.1f}MB',
             xy=(1, mean_dbon_max), xytext=(10, 0), textcoords='offset points',
             fontsize=11, color='#c0392b', va='center')
delta_max = mean_dbon_max - mean_dboff_max
ax3.annotate(f'Δ = {"+" if delta_max >= 0 else ""}{delta_max:.1f}MB',
             xy=(0.5, (mean_dboff_max + mean_dbon_max) / 2),
             xytext=(0.5, 15), textcoords='data',
             fontsize=12, color='#2c3e50', ha='center', weight='bold',
             bbox=dict(boxstyle='round,pad=0.5', facecolor='#fff3cd', edgecolor='#856404'))
ax3.set_xticks([0, 1])
ax3.set_xticklabels(x_labels, fontsize=10)
ax3.set_xlim(-0.5, 1.5)
ax3.set_ylim(0, 70)
ax3.set_ylabel('Heap peak (MB)', fontsize=11)
ax3.set_title('Peak (max) — does DB raise it?', fontsize=12)
ax3.grid(True, alpha=0.3, axis='y')

plt.suptitle('V3-F: DB writes on/off comparison — fixed -Xmx64m, batch=1000, 5 trials each',
             fontsize=13)
out_path = os.path.join(script_dir, 'v3-heap-db.png')
plt.savefig(out_path, dpi=140, bbox_inches='tight')
print(f'Saved: {out_path}')
print()
print(f'{"":>10} {"min mean±std":>14} {"max mean±std":>14} {"time mean±std":>16}')
print(f'{"DB off":>10} '
      f'{mean(dboff_mins):>6.1f}±{stdev(dboff_mins):.1f}MB '
      f'{mean(dboff_maxes):>6.1f}±{stdev(dboff_maxes):.1f}MB '
      f'{mean(dboff_times):>6.2f}±{stdev(dboff_times):.2f}s')
print(f'{"DB on":>10} '
      f'{mean(dbon_mins):>6.1f}±{stdev(dbon_mins):.1f}MB '
      f'{mean(dbon_maxes):>6.1f}±{stdev(dbon_maxes):.1f}MB '
      f'{mean(dbon_times):>6.2f}±{stdev(dbon_times):.2f}s')
print()
print(f'  Δ floor (DB cost): +{delta_min:.1f}MB')
print(f'  Δ peak           : {"+" if delta_max >= 0 else ""}{delta_max:.1f}MB')
