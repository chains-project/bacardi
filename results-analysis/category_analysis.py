import os
import json
import sys
import matplotlib.pyplot as plt
import csv

def process_results(results_path):
    # Initialize the failure category counts
    failure_counts = {}

    # Process each prompt folder in the results folder
    for prompt_folder in os.listdir(results_path):
        if prompt_folder.startswith('.'):
            continue
        prompt_folder_path = os.path.join(results_path, prompt_folder)
        prompt_name = prompt_folder.replace("results_","")
        if os.path.isdir(prompt_folder_path):
            # Process each LLM folder in the prompt folder
            for llm_folder in os.listdir(prompt_folder_path):
                if llm_folder.startswith('.'):
                    continue
                llm_folder_path = os.path.join(prompt_folder_path, llm_folder)
                if os.path.isdir(llm_folder_path):
                    # Find the JSON file
                    for file in os.listdir(llm_folder_path):
                        if file.endswith('.json'):
                            json_file_path = os.path.join(llm_folder_path, file)
                            with open(json_file_path, 'r') as json_file:
                                data = json.load(json_file)
                                for commit, details in data.items():
                                    for attempt in details['attempts']:
                                        failure_category = attempt['failureCategory']
                                        if prompt_name not in failure_counts:
                                            failure_counts[prompt_name] = {}
                                        if failure_category not in failure_counts[prompt_name]:
                                            failure_counts[prompt_name][failure_category] = {}
                                        if llm_folder not in failure_counts[prompt_name][failure_category]:
                                            failure_counts[prompt_name][failure_category][llm_folder] = 0
                                        failure_counts[prompt_name][failure_category][llm_folder] += 1

    return failure_counts

def plot_results(failure_counts, output_dir):
    plotbycat={}
    for prompt_name in failure_counts:
        for category in failure_counts[prompt_name]:
            for llm_folder in failure_counts[prompt_name][category]:
                if category not in failure_counts[prompt_name]:
                    continue
                if category not in plotbycat:
                    plotbycat[category]={}
                if prompt_name not in plotbycat[category]:
                    plotbycat[category][prompt_name]={}
                if llm_folder not in failure_counts[prompt_name][category]:
                        continue    
                if llm_folder not in plotbycat[category][prompt_name]:
                    plotbycat[category][prompt_name][llm_folder]=failure_counts[prompt_name][category][llm_folder]  
                
    for category, prompts in plotbycat.items():
        plt.figure()
        prompt_names = list(prompts.keys())
        llm_names = set()
        for prompt in prompt_names:
            llm_names.update(prompts[prompt].keys())
        llm_names = sorted(llm_names)
        
        bar_width = 0.1
        index = range(len(prompt_names))
        
        for i, llm in enumerate(llm_names):
            counts = [prompts[prompt].get(llm, 0) for prompt in prompt_names]
            plt.bar([p + i * bar_width for p in index], counts, bar_width, label=llm)
        
        plt.xlabel('Prompt')
        plt.ylabel('Count')
        plt.title(f'Failure Category: {category}')
        plt.xticks([p + bar_width * (len(llm_names) / 2) for p in index], prompt_names, rotation=45, fontsize=10)
        plt.legend(bbox_to_anchor=(1.05, 1), loc='upper left')
        plt.tight_layout()
        plt.savefig(os.path.join(output_dir, f'{category}.png'))
        plt.close()

def output_results_to_csv(failure_counts, output_dir):
    csv_file_path = os.path.join(output_dir, 'failure_counts.csv')
    with open(csv_file_path, 'w', newline='') as csv_file:
        writer = csv.writer(csv_file)
        writer.writerow(['Prompt', 'Failure_Category', 'LLM', 'Count'])
        for prompt, categories in failure_counts.items():
            for category, llms in categories.items():
                for llm, count in llms.items():
                    writer.writerow([prompt, category, llm, count])

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python build_success.py <results_path>")
        sys.exit(1)

    results_path = sys.argv[1]

    failure_counts = process_results(results_path)
    output_dir = 'results-analysis-frank'
    os.makedirs(output_dir, exist_ok=True)
    plot_results(failure_counts, output_dir)
    output_results_to_csv(failure_counts, output_dir)
    print(f"Plots and CSV saved in {output_dir}")
