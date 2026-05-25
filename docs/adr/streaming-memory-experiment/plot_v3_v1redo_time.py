"""
V1 redo — TIME AXIS — GC log as main source, wall-clock synchronized.

Both sources now share absolute wall-clock time:
- CSV: wall_ms column = epoch millis at each batch flush
- GC log: ISO 8601 timestamp via -Xlog:gc*:file=...:time

t=0 = streamOnce start = first CSV wall_ms - elapsed_ms.

Produces v3-heap-v1redo-time.png.
"""
import csv
import os
import re
from datetime import datetime
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches

script_dir = os.path.dirname(os.path.abspath(__file__))

# --- load batch flush CSV ---
csv_data = []
with open(os.path.join(script_dir, 'v3-heap-64m-v1redo.csv')) as f:
    for row in csv.DictReader(f):
        csv_data.append({
            'count': int(row['count']),
            'heap': int(row['heap_used_mb']),
            'elapsed_ms': int(row['elapsed_ms']),
            'wall_ms': int(row['wall_ms']),
        })

# t=0 = streamOnce start = first wall_ms - elapsed_ms
stream_start_wall_ms = csv_data[0]['wall_ms'] - csv_data[0]['elapsed_ms']
streaming_end_ms = csv_data[-1]['elapsed_ms']

# --- load time-based heap samples (10ms interval) ---
samples = []
samples_path = os.path.join(script_dir, 'v3-heap-64m-v1redo-samples.csv')
with open(samples_path) as f:
    for row in csv.DictReader(f):
        samples.append({
            'rel_ms': int(row['wall_ms']) - stream_start_wall_ms,
            'heap': int(row['heap_used_mb']),
        })

# --- parse GC log ---
gc_pattern = re.compile(
    r'\[([\dT:.+-]+)\]\[info\]\[gc\s*\] GC\(\d+\) Pause (Young|Full) \(([^)]+)\).*?(\d+)M->(\d+)M\(\d+M\)\s+(\d+\.\d+)ms'
)


def iso_to_epoch_ms(ts):
    # python's fromisoformat (3.11+) handles +0900; normalize to +09:00 for safety
    if re.match(r'.*[+-]\d{4}$', ts):
        ts = ts[:-2] + ':' + ts[-2:]
    return datetime.fromisoformat(ts).timestamp() * 1000.0


all_gcs = []
with open(os.path.join(script_dir, 'gc-v1-redo.log')) as f:
    for line in f:
        m = gc_pattern.search(line)
        if m:
            t_end_wall_ms = iso_to_epoch_ms(m.group(1))
            all_gcs.append({
                't_end_wall_ms': t_end_wall_ms,
                't_end_rel_ms': t_end_wall_ms - stream_start_wall_ms,
                'subtype': m.group(3),
                'before': int(m.group(4)),
                'after': int(m.group(5)),
                'duration_ms': float(m.group(6)),
            })

# filter to streamOnce window (with small margin)
gc_events = [g for g in all_gcs
             if -100 <= g['t_end_rel_ms'] <= streaming_end_ms + 100]
for g in gc_events:
    g['t_start_rel_ms'] = g['t_end_rel_ms'] - g['duration_ms']

# --- plot ---
gc_colors = {
    'Normal':           '#3498db',
    'Prepare Mixed':    '#f39c12',
    'Mixed':            '#e74c3c',
    'Concurrent Start': '#27ae60',
}

fig, ax = plt.subplots(figsize=(14, 7))

# main curve = time-based heap samples (10ms interval) — sees JIT warm-up area too
sample_x = [s['rel_ms'] for s in samples]
sample_y = [s['heap'] for s in samples]
ax.plot(sample_x, sample_y, color='#2c3e50', linewidth=1.0, zorder=2,
        label=f'Heap (10ms time-based samples, {len(samples)} pts)')
ax.fill_between(sample_x, 0, sample_y, color='#3498db', alpha=0.08)

# batch flush events as separate dots — shows when JDBC flush happened
flush_x = [d['elapsed_ms'] for d in csv_data]
flush_y = [d['heap'] for d in csv_data]
ax.scatter(flush_x, flush_y, color='#e67e22', alpha=0.7, s=20, zorder=3,
           marker='o', edgecolor='white', linewidth=0.5,
           label=f'Batch flush points ({len(csv_data)} pts)')

# GC events: vertical line + marker at 'after'
for g in gc_events:
    color = gc_colors.get(g['subtype'], '#7f8c8d')
    ax.axvline(x=g['t_end_rel_ms'], color=color, linewidth=0.5, alpha=0.35, zorder=1)
    ax.scatter([g['t_end_rel_ms']], [g['after']], color=color, s=90, zorder=4,
               edgecolor='white', linewidth=1.5, marker='v')

ax.axhline(y=64, color='gray', linestyle='--', linewidth=0.8, alpha=0.6,
           label='-Xmx64m limit')

ax.set_xlabel('Time (ms, since streamOnce start, wall-clock synced)', fontsize=11)
ax.set_ylabel('Heap usage (MB)', fontsize=11)
ax.set_title('V1 redo — TIME AXIS — 10ms heap samples (line) + flush points (orange) + GC events (triangles)\n'
             f'118,122 entries, -Xmx64m, batch=1000, DB off — '
             f'streaming {streaming_end_ms}ms, {len(gc_events)} GC events in window',
             fontsize=12, pad=15)

ax.set_ylim(0, 70)
x_max = max(streaming_end_ms, gc_events[-1]['t_end_rel_ms'] if gc_events else 0)
ax.set_xlim(-50, x_max * 1.05)
ax.grid(True, alpha=0.3)

gc_legend_handles = [mpatches.Patch(color=c, label=n) for n, c in gc_colors.items()]
legend1 = ax.legend(handles=gc_legend_handles, loc='upper left',
                     title='GC Event Type', framealpha=0.95)
ax.add_artist(legend1)
ax.legend(loc='lower right', framealpha=0.95)

gc_count_by_type = {}
for g in gc_events:
    gc_count_by_type[g['subtype']] = gc_count_by_type.get(g['subtype'], 0) + 1
stats = (f'{len(csv_data)} flushes / {streaming_end_ms}ms streaming\n'
         f'{len(gc_events)} GC in window: ' + ', '.join(f'{n}={c}' for n, c in gc_count_by_type.items()) + '\n'
         f't=0 = streamOnce start (wall {stream_start_wall_ms:.0f}ms)')
ax.text(0.02, 0.10, stats, transform=ax.transAxes, fontsize=9,
        verticalalignment='top', family='monospace',
        bbox=dict(boxstyle='round,pad=0.5', facecolor='#ecf0f1',
                  edgecolor='#95a5a6', alpha=0.95))

plt.tight_layout()
out_path = os.path.join(script_dir, 'v3-heap-v1redo-time.png')
plt.savefig(out_path, dpi=140, bbox_inches='tight')
print(f'Saved: {out_path}')
print(f'  Stream start wall_ms: {stream_start_wall_ms:.0f}')
print(f'  Streaming duration: {streaming_end_ms}ms')
print(f'  Total GC events in log: {len(all_gcs)}, in streamOnce window: {len(gc_events)}')
print(f'  GC types in window: {gc_count_by_type}')
print(f'  GC times in window (ms): {[round(g["t_end_rel_ms"], 1) for g in gc_events]}')
