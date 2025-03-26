import json
import os

import numpy as np
import csv
import pandas as pd
import matplotlib.pyplot as plt


output_csv = False
output_excel = False
output_latex = True
output_pgfkeys = False

api_diff_path = '/Users/mam10532/Documents/GitHub/bacardi/analysis/api_diff.txt'

root_folder_path = '/Users/mam10532/Documents/GitHub/bacardi/results'
output_folder='rq3-overall-output/'

# Check if the output folder exists, if not, create it
if not os.path.exists(output_folder):
    os.makedirs(output_folder)

prompt_aliases = {
    "results_baseline_buggy_line": "P2",
    "results_baseline_api_diff_cot": "P7",
    "results_baseline_cot": "P5",
    "results_baseline": "P1",
    "results_baseline_buggy_line_api_diff": "P4",
    "results_baseline_api_diff": "P3",
    "results_baseline_cot_buggy_line": "P6",
    "results_baseline_api_diff_cot_buggy": "P8"
}
prompt_latex = {
    "results_baseline_buggy_line": "$P_2$",
    "results_baseline_api_diff_cot": "$P_7$",
    "results_baseline_cot": "$P_5$",
    "results_baseline": "$P_1$",
    "results_baseline_buggy_line_api_diff": "$P_4$",
    "results_baseline_api_diff": "$P_3$",
    "results_baseline_cot_buggy_line": "$P_6$",
    "results_baseline_api_diff_cot_buggy": "$P_8$"
}

llm_mapping = {
    "deepseek": "\\textbf{Deepseek V3}",
    "gemini": "\\textbf{Gemini-2.0-flash}",
    "gpt": "\\textbf{Gpt-4o-mini}",
    "o3": "\\textbf{Gpt-4o-mini}",
    "qwen": "\\textbf{Qwen2.5-32b-instruct}"
}

llm_mapping_pgfkeys = {
    "gemini-2.0-flash-001": "gemini",
    "o3-mini-2025-01-31": "o3-mini",
    "gpt-4o-mini": "gpt",
    "deepseek-deepseek-chat": "deepseek",
    "qwen-qwen2.5-32b-instruct": "qwen"
}

# Read the lines from the text file
with open(api_diff_path, 'r') as file:
    lines = [line.strip() for line in file.readlines()]

## print all results
print(f' Number of lines {len(lines)}')

# Dictionary to store the results
results = {}
total_commits = 0
llm_mapping = {
    "deepseek": "\\textbf{Deepseek V3}",
    "gemini": "\\textbf{Gemini-2.0-flash}",
    "gpt": "\\textbf{Gpt-4o-mini}",
    "o3-mini": "\\textbf{o3-mini}",
    "qwen": "\\textbf{Qwen2.5-32b-instruct}"
}

# Iterate over the prompts in the root folder
for prompt_folder in os.listdir(root_folder_path):
    prompt_path = os.path.join(root_folder_path, prompt_folder)
    if os.path.isdir(prompt_path):
        commit_results = {}
        prompt_results = {}
        # Iterate over the models
        for model_folder in os.listdir(prompt_path):
            model_path = os.path.join(prompt_path, model_folder)
            if os.path.isdir(model_path):
                print(f'Model {prompt_folder}/{model_folder}')
                json_file_path = os.path.join(model_path, f'result_repair_{model_folder}.json')
                if os.path.isfile(json_file_path):
                    with open(json_file_path, 'r') as json_file:
                        data = json.load(json_file)
                        print(f'JSON: {len(data)}')
                        total_prefix_errors = 0
                        total_fixed_errors = 0
                        total_new_errors = 0
                        relative_fixed_percentage=0
                        for commit, details in data.items():
                            if commit in lines:
                                total_commits += 1
                                for attempt in details['attempts']:
                                    if attempt['failureCategory'] == "ERROR_MODEL_RESPONSE" or attempt['failureCategory'] == "NOT_REPAIRED":
                                        total_prefix_errors += attempt['prefixErrors']
                                        total_fixed_errors += 0
                                        total_new_errors +=0
                                    else:
                                        total_prefix_errors += attempt['prefixErrors']
                                        total_fixed_errors += attempt['fixedErrors']
                                        total_new_errors += attempt['newErrors']

                        print(f'Commits: {total_commits}')
                        # Calculate the percentage of fixed files
                        if total_prefix_errors> 0:
                            relative_fixed_percentage = ((total_fixed_errors  - total_new_errors)/ total_prefix_errors) * 100
                        else:
                            relative_fixed_percentage = 0

                        model=llm_mapping_pgfkeys.get(model_folder, model_folder)
                        # Store the results for each model
                        prompt_results[model] = {
                            'total_prefix_errors': total_prefix_errors,
                            'total_fixed_errors': total_fixed_errors,
                            'total_new_errors': total_new_errors,
                            'relative_fixed_percentage': relative_fixed_percentage
                        }

        results[prompt_folder] = prompt_results


# Write the results to a CSV file
if output_csv:
    csv_file_path = os.path.join(output_folder, 'rq3-results.csv')
    with open(csv_file_path, 'w') as csv_file:
        writer = csv.writer(csv_file)
        writer.writerow(["Prompt", "Model", "Total Prefix Errors", "Total Fixed Errors", "Total New Errors", "Relative Fixed Percentage"])
        for prompt, models in results.items():
            for model, result in models.items():
                writer.writerow([prompt, model, result['total_prefix_errors'], result['total_fixed_errors'], result['total_new_errors'], result['relative_fixed_percentage']])
        print(f"Results written to {csv_file_path}")


# Write the results to an Excel file
if output_excel:
    excel_file_path = os.path.join(output_folder, 'rq3-results.xlsx')
    with pd.ExcelWriter(excel_file_path) as writer:
        for prompt, models in results.items():
            df = pd.DataFrame(models).T
            df.index.name = "Model"
            df.columns = ["Total Prefix Errors", "Total Fixed Errors", "Total New Errors", "Relative Fixed Percentage"]
            df.to_excel(writer, sheet_name=prompt_aliases.get(prompt, prompt))
        print(f"Results written to {excel_file_path}")


highest_value=0
highest_key=""     
# Print the results
pgfkeys_output = []
for prompt, models in results.items():
    prompt_alias = prompt_aliases.get(prompt, prompt)
    print(f"Prompt: {prompt_alias}")
    
    for model, result in models.items():
        print(f"  Model: {model}")
        print(f"    Total Prefix Errors: {result['total_prefix_errors']}")
        print(f"    Total Fixed Errors: {result['total_fixed_errors']}")
        print(f"    Total New Errors: {result['total_new_errors']}")
        print(f"    Relative Fixed Percentage: {result['relative_fixed_percentage']:.2f}%")
        value=result['relative_fixed_percentage']
        if value>highest_value:
            highest_value=value
            highest_key=f'{prompt_alias}_{model}'
        if output_pgfkeys:
            pgfkeys_output.append(f'\\pgfkeyssetvalue{{{prompt_alias}_{model}_total_prefix}}{{{result["total_prefix_errors"]}}}')
            pgfkeys_output.append(f'\\pgfkeyssetvalue{{{prompt_alias}_{model}_total_fixed}}{{{result["total_fixed_errors"]}}}')
            pgfkeys_output.append(f'\\pgfkeyssetvalue{{{prompt_alias}_{model}_total_new}}{{{result["total_new_errors"]}}}')
            pgfkeys_output.append(f'\\pgfkeyssetvalue{{{prompt_alias}_{model}_relative_fixed_percentage}}{{{result["relative_fixed_percentage"]:.2f}}}')
            relative=result['total_fixed_errors'] - result['total_new_errors']
            pgfkeys_output.append(f'\\pgfkeyssetvalue{{{prompt_alias}_{model}_relative_nominator}}{{{relative}}}')
    print()

# Write pgfkeys to a file
if output_pgfkeys:
    pgfkeys_file_path = os.path.join(output_folder, 'rq3-pgfkeys_output.tex')
    with open(pgfkeys_file_path, 'w') as pgfkeys_file:
        pgfkeys_file.write('\n'.join(pgfkeys_output))

if output_latex:
    # Prepare data for LaTeX table
    table_data = []
    prompts = sorted(results.keys(), key=lambda x: prompt_aliases.get(x, x))
    headers = ["Prompt"] + [llm_mapping.get(model, model) for model in sorted(next(iter(results.values())).keys())]

    for prompt in prompts:
        row = [prompt_latex.get(prompt, prompt)]
        for model in sorted(next(iter(results.values())).keys()):
            result = results[prompt][model]
            key_base = f"{prompt_aliases.get(prompt, prompt)}_{model}"
            if key_base == highest_key:
                row.append(f"\\textbf{{\\pgfkeysvalueof{{{prompt_aliases.get(prompt, prompt)}_{model}_relative_nominator}}/{{\\pgfkeysvalueof{{{prompt_aliases.get(prompt, prompt)}_{model}_total_prefix}}}} (\\pgfkeysvalueof{{{prompt_aliases.get(prompt, prompt)}_{model}_relative_fixed_percentage}}\\%)}}")
            else:
                row.append(f"\\pgfkeysvalueof{{{prompt_aliases.get(prompt, prompt)}_{model}_relative_nominator}}/{{\\pgfkeysvalueof{{{prompt_aliases.get(prompt, prompt)}_{model}_total_prefix}}}} (\\pgfkeysvalueof{{{prompt_aliases.get(prompt, prompt)}_{model}_relative_fixed_percentage}}\\%)")
        table_data.append(row)

    # Generate LaTeX table without highlighted maximum values
    latex_table = "\\begin{tabular}{l" + "r" * len(headers[1:]) + "}\n"
    latex_table += " & ".join(headers) + " \\\\\n"
    latex_table += "\\hline\n"
    for row in table_data:
        latex_table += " & ".join(row) + " \\\\\n"
    latex_table += "\\end{tabular}"

    print(latex_table)
    # Write LaTeX table to a file in the output folder
    latex_file_path = os.path.join(output_folder, "rq3-latex_table.tex")
    with open(latex_file_path, 'w') as latex_file:
        latex_file.write(latex_table)
