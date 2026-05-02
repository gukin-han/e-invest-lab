"""
JVM G1GC heap pattern visualizer for DART corpCode.xml streaming parse experiment.

Run: python3 plot_heap.py
Output: heap-pattern.png (saved next to this script)

Data captured from CorpCodeFetchSmokeTest with -Xms64m -Xmx64m -Xlog:gc.
117,496 companies parsed via StAX streaming, batch size 1000.
"""
import os
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches

# heap data (count, heap_MB)
data = [
    (500,29),(1000,30),(1500,31),(2000,31),(2500,31),(3000,32),(3500,32),(4000,33),
    (4500,33),(5000,34),(5500,34),(6000,35),(6500,36),(7000,36),(7500,36),(8000,37),
    (8500,37),(9000,38),(9500,38),(10000,39),(10500,39),(11000,40),(11500,40),(12000,41),
    (12500,41),(13000,42),(13500,42),(14000,43),(14500,43),(15000,44),(15500,44),(16000,45),
    (16500,45),(17000,46),(17500,46),(18000,47),(18500,47),
    (19000,25),(19500,25),(20000,26),(20500,26),(21000,27),(21500,27),(22000,28),(22500,28),
    (23000,29),(23500,29),(24000,30),(24500,30),(25000,30),(25500,31),(26000,31),(26500,32),
    (27000,32),(27500,33),(28000,33),(28500,34),(29000,34),(29500,34),(30000,35),(30500,35),
    (31000,36),(31500,36),(32000,37),(32500,37),(33000,37),(33500,38),(34000,38),(34500,39),
    (35000,39),(35500,40),(36000,40),(36500,41),(37000,41),(37500,41),(38000,42),(38500,42),
    (39000,43),(39500,43),(40000,44),(40500,44),(41000,45),(41500,45),(42000,45),(42500,46),
    (43000,46),(43500,47),(44000,47),(44500,48),(45000,48),(45500,48),
    (46000,33),(46500,33),(47000,34),(47500,34),(48000,35),(48500,35),(49000,36),(49500,36),
    (50000,37),(50500,37),(51000,37),(51500,38),(52000,38),(52500,39),(53000,39),(53500,40),
    (54000,40),(54500,40),(55000,41),(55500,41),(56000,42),(56500,42),(57000,43),(57500,43),
    (58000,44),(58500,44),(59000,45),(59500,45),(60000,46),(60500,46),(61000,46),(61500,47),
    (62000,47),(62500,48),(63000,48),(63500,48),(64000,49),(64500,49),
    (65000,39),(65500,39),(66000,40),(66500,40),(67000,40),(67500,41),(68000,41),(68500,42),
    (69000,42),(69500,43),(70000,43),(70500,44),(71000,44),(71500,44),(72000,45),(72500,45),
    (73000,45),(73500,46),(74000,47),(74500,47),(75000,47),(75500,48),(76000,48),(76500,49),
    (77000,49),(77500,50),(78000,50),(78500,50),(79000,51),(79500,51),(80000,51),
    (80500,44),(81000,45),(81500,45),(82000,45),(82500,46),(83000,46),(83500,47),(84000,47),
    (84500,48),(85000,48),(85500,48),(86000,49),(86500,49),(87000,50),(87500,51),(88000,51),
    (88500,52),(89000,52),(89500,52),(90000,53),(90500,53),
    (91000,47),(91500,47),(92000,48),(92500,48),(93000,49),(93500,49),(94000,49),(94500,50),
    (95000,50),(95500,51),(96000,51),(96500,52),(97000,52),(97500,52),(98000,53),(98500,53),
    (99000,54),(99500,54),(100000,55),(100500,55),
    (101000,55),(101500,50),(102000,51),(102500,51),(103000,52),(103500,52),(104000,53),
    (104500,53),(105000,54),(105500,54),(106000,54),(106500,55),(107000,55),(107500,56),
    (108000,52),(108500,53),(109000,53),(109500,54),(110000,54),(110500,54),(111000,55),
    (111500,55),(112000,56),(112500,56),(113000,57),(113500,57),
    (114000,54),(114500,55),(115000,55),(115500,56),(116000,56),(116500,57),(117000,57),
]

counts = [d[0] for d in data]
heaps = [d[1] for d in data]

# GC events: (count_after_drop, type, before_MB, after_MB)
gc_events = [
    (19000, 'Prepare Mixed', 47, 25),
    (46000, 'Mixed', 48, 33),
    (65000, 'Concurrent Start', 49, 39),
    (80500, 'Prepare Mixed', 51, 44),
    (91000, 'Mixed', 53, 47),
    (101500, 'Concurrent Start', 55, 50),
    (108000, 'Young Normal', 56, 52),
    (114000, 'Prepare Mixed', 57, 54),
]

# colors per GC type
gc_colors = {
    'Young Normal':       '#3498db',  # blue
    'Prepare Mixed':      '#f39c12',  # orange
    'Mixed':              '#e74c3c',  # red
    'Concurrent Start':   '#27ae60',  # green
}

fig, ax = plt.subplots(figsize=(14, 7))

# main heap line
ax.plot(counts, heaps, color='#2c3e50', linewidth=1.2, zorder=2, label='Heap usage')

# heap limit line
ax.axhline(y=64, color='gray', linestyle='--', linewidth=0.8, alpha=0.6, label='-Xmx64m limit')

# fill under line
ax.fill_between(counts, 0, heaps, color='#3498db', alpha=0.08)

# annotate GC events
for count, gc_type, before, after in gc_events:
    color = gc_colors[gc_type]
    ax.axvline(x=count, color=color, linewidth=0.8, alpha=0.5, zorder=1)
    ax.scatter([count], [after], color=color, s=80, zorder=3, edgecolor='white', linewidth=1.5)

# legend for GC types
legend_handles = [
    mpatches.Patch(color=color, label=name)
    for name, color in gc_colors.items()
]
legend1 = ax.legend(handles=legend_handles, loc='upper left', title='GC Event Type', framealpha=0.95)
ax.add_artist(legend1)

# main legend (heap line + limit)
ax.legend(loc='lower right', framealpha=0.95)

# labels and title
ax.set_xlabel('Processed entries', fontsize=11)
ax.set_ylabel('Heap usage (MB)', fontsize=11)
ax.set_title('JVM G1GC Heap Pattern — DART corpCode.xml streaming parse\n'
             '117,496 companies processed under -Xmx64m, batch size 1000',
             fontsize=12, pad=15)

ax.set_ylim(0, 70)
ax.set_xlim(0, 120000)
ax.grid(True, alpha=0.3)

# steady state annotation
ax.axhspan(54, 57, color='red', alpha=0.05, zorder=0)
ax.text(115000, 60, 'Steady state\n(54~57MB)',
        fontsize=9, color='#c0392b', ha='right',
        bbox=dict(boxstyle='round,pad=0.4', facecolor='white', edgecolor='#c0392b', alpha=0.9))

# ramp-up annotation
ax.annotate('Ramp-up\n(young gen → old gen tenuring)',
            xy=(10000, 38), xytext=(25000, 12),
            fontsize=9, color='#2c3e50',
            arrowprops=dict(arrowstyle='->', color='#2c3e50', alpha=0.6),
            bbox=dict(boxstyle='round,pad=0.4', facecolor='white', edgecolor='#7f8c8d', alpha=0.9))

# first major drop annotation
ax.annotate('First Mixed GC prep\n(47MB → 25MB)',
            xy=(19000, 25), xytext=(35000, 60),
            fontsize=9, color='#c0392b',
            arrowprops=dict(arrowstyle='->', color='#c0392b', alpha=0.6),
            bbox=dict(boxstyle='round,pad=0.4', facecolor='white', edgecolor='#c0392b', alpha=0.9))

plt.tight_layout()

# save next to this script
script_dir = os.path.dirname(os.path.abspath(__file__))
output_path = os.path.join(script_dir, 'heap-pattern.png')
plt.savefig(output_path, dpi=140, bbox_inches='tight')
print(f"Saved: {output_path}")
