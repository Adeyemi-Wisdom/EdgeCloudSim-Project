import pandas as pd
import matplotlib.pyplot as plt

# datasets
old_data_text = """2343;37;13;0;1.8874605940162614;1.8683637122541605;0.01909688176210396;0.0;0.0;15;22;0.0;0.0;0
2343;37;13;0;1.8874605940162614;1.8683637122541605;0.0;7.1392496392496385;15
0;0;0;0;0.0;0.0;0.0;0.0;0
0;0;0;0;0.0;0.0;0.0;0.0;0
0.01909688176210396;0.0;0.0;0.0;0;0;0;0
101;0.0"""

new_data_text = """2852;16;34;0;1.2668087863454718;1.0194998402790258;0.24730894606645037;0.0;0.0;0;16;0.0;0.0;0
1422;11;8;0;1.8614970261921988;1.842420853532732;0.0;4.451659451659451;0
1430;5;0;0;0.6754474737146712;0.20118258094561783;0.0;0.2702020202020202;0
0;0;26;0;0.0;0.0;0.0;0.0;0
0.01907617265946825;0.0;0.47426489276905653;0.0;0;0;0;0
59;0.0"""

cols = [
    "total_tasks", "tasks_completed", "tasks_failed", "tasks_mobility_fail",
    "avg_service_time", "avg_processing_time", "avg_network_delay",
    "energy_consumption", "avg_vm_load", "mobility_fail", "capacity_fail",
    "bandwidth_fail", "unknown_fail", "spare"
]

old_df = pd.read_csv(pd.io.common.StringIO(old_data_text), sep=';', names=cols)
new_df = pd.read_csv(pd.io.common.StringIO(new_data_text), sep=';', names=cols)
old_df.fillna(0, inplace=True)
new_df.fillna(0, inplace=True)

# METRIC FUNCTIONS 
def task_success_ratio(df):
    total = df['total_tasks'].sum()
    success = df['tasks_completed'].sum()
    return (success / total) * 100 if total > 0 else 0

def avg_latency(df):
    return df[['avg_service_time', 'avg_processing_time', 'avg_network_delay']].mean()

def avg_vm_load(df):
    return df['avg_vm_load'].mean()

def avg_energy(df):
    return df['energy_consumption'].mean()

def failure_breakdown(df):
    return {
        'Mobility': df['mobility_fail'].sum(),
        'Capacity': df['capacity_fail'].sum(),
        'Bandwidth': df['bandwidth_fail'].sum(),
        'Unknown': df['unknown_fail'].sum()
    }

# METRIC VALUES
metrics = {
    'Success Ratio (%)': [task_success_ratio(old_df), task_success_ratio(new_df)],
    'Avg Service Time (s)': [old_df['avg_service_time'].mean(), new_df['avg_service_time'].mean()],
    'Avg Processing Time (s)': [old_df['avg_processing_time'].mean(), new_df['avg_processing_time'].mean()],
    'Avg Network Delay (s)': [old_df['avg_network_delay'].mean(), new_df['avg_network_delay'].mean()],
    'Avg VM Load': [avg_vm_load(old_df), avg_vm_load(new_df)],
    'Avg Energy Consumption': [avg_energy(old_df), avg_energy(new_df)],
}
fail_old = failure_breakdown(old_df)
fail_new = failure_breakdown(new_df)

# INDIVIDUAL VISUALS
def save_bar_chart(title, ylabel, data, filename):
    strategies = ['Old Strategy', 'New EO Strategy']
    plt.figure(figsize=(6, 4))
    plt.bar(strategies, data, color=['#8888ff', '#4caf50'])
    plt.title(title)
    plt.ylabel(ylabel)
    plt.grid(axis='y', linestyle='--', alpha=0.7)
    plt.tight_layout()
    plt.savefig(filename)
    plt.close()

# Task success ratio
save_bar_chart("Task Success Ratio", "Percentage (%)",
               metrics['Success Ratio (%)'], "task_success_ratio.png")

# Latency
plt.figure(figsize=(7, 4))
labels = ['Service Time', 'Processing Time', 'Network Delay']
x = range(len(labels))
old_vals = [metrics['Avg Service Time (s)'][0],
            metrics['Avg Processing Time (s)'][0],
            metrics['Avg Network Delay (s)'][0]]
new_vals = [metrics['Avg Service Time (s)'][1],
            metrics['Avg Processing Time (s)'][1],
            metrics['Avg Network Delay (s)'][1]]
plt.bar([i - 0.2 for i in x], old_vals, 0.4, label='Old', color='#8888ff')
plt.bar([i + 0.2 for i in x], new_vals, 0.4, label='New EO', color='#4caf50')
plt.xticks(x, labels)
plt.ylabel('Seconds')
plt.title('Latency Components Comparison')
plt.legend()
plt.grid(axis='y', linestyle='--', alpha=0.7)
plt.tight_layout()
plt.savefig("latency_components.png")
plt.close()

# VM Load
save_bar_chart("Average VM Load", "Load (avg units)",
               metrics['Avg VM Load'], "vm_load.png")

# Energy
save_bar_chart("Average Energy Consumption", "Energy Units",
               metrics['Avg Energy Consumption'], "energy_consumption.png")

# Failures
labels = list(fail_old.keys())
x = range(len(labels))
plt.figure(figsize=(7, 4))
plt.bar([i - 0.2 for i in x], fail_old.values(), 0.4, label='Old', color='#8888ff')
plt.bar([i + 0.2 for i in x], fail_new.values(), 0.4, label='New EO', color='#4caf50')
plt.xticks(x, labels)
plt.ylabel('Failure Count')
plt.title('Failure Causes Comparison')
plt.legend()
plt.grid(axis='y', linestyle='--', alpha=0.7)
plt.tight_layout()
plt.savefig("failure_causes.png")
plt.close()

# DASHBOARD 
fig, axes = plt.subplots(2, 3, figsize=(12, 7))
fig.suptitle('Strategy Comparison Dashboard', fontsize=14, fontweight='bold')

#  Success Ratio
axes[0,0].bar(['Old', 'New'], metrics['Success Ratio (%)'], color=['#8888ff','#4caf50'])
axes[0,0].set_title('Task Success Ratio (%)')
axes[0,0].grid(axis='y', linestyle='--', alpha=0.7)

#  Service & Processing Time
axes[0,1].bar(['Old Service', 'New Service'], [metrics['Avg Service Time (s)'][0], metrics['Avg Service Time (s)'][1]],
              color=['#8888ff','#4caf50'])
axes[0,1].bar(['Old Proc', 'New Proc'], [metrics['Avg Processing Time (s)'][0], metrics['Avg Processing Time (s)'][1]],
              color=['#9999ff','#66bb6a'])
axes[0,1].set_title('Service & Processing Time')
axes[0,1].grid(axis='y', linestyle='--', alpha=0.7)

#  Network Delay
axes[0,2].bar(['Old', 'New'], [metrics['Avg Network Delay (s)'][0], metrics['Avg Network Delay (s)'][1]],
              color=['#8888ff','#4caf50'])
axes[0,2].set_title('Network Delay (s)')
axes[0,2].grid(axis='y', linestyle='--', alpha=0.7)

#  VM Load
axes[1,0].bar(['Old', 'New'], metrics['Avg VM Load'], color=['#8888ff','#4caf50'])
axes[1,0].set_title('Average VM Load')
axes[1,0].grid(axis='y', linestyle='--', alpha=0.7)

#  Energy Consumption
axes[1,1].bar(['Old', 'New'], metrics['Avg Energy Consumption'], color=['#8888ff','#4caf50'])
axes[1,1].set_title('Energy Consumption')
axes[1,1].grid(axis='y', linestyle='--', alpha=0.7)

# Failure Breakdown
labels = list(fail_old.keys())
x = range(len(labels))
axes[1,2].bar([i - 0.2 for i in x], fail_old.values(), 0.4, label='Old', color='#8888ff')
axes[1,2].bar([i + 0.2 for i in x], fail_new.values(), 0.4, label='New EO', color='#4caf50')
axes[1,2].set_xticks(x)
axes[1,2].set_xticklabels(labels)
axes[1,2].set_title('Failure Causes')
axes[1,2].legend(fontsize=8)
axes[1,2].grid(axis='y', linestyle='--', alpha=0.7)

plt.tight_layout(rect=[0, 0, 1, 0.96])
plt.savefig("strategy_comparison_dashboard.png", dpi=300)
plt.close()

print(" All charts generated successfully:")
print("   - task_success_ratio.png")
print("   - latency_components.png")
print("   - vm_load.png")
print("   - energy_consumption.png")
print("   - failure_causes.png")
print("   - strategy_comparison_dashboard.png (Dashboard View)")

