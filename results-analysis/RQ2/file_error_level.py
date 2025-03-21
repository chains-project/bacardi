import json
import os
from tabulate import tabulate
import matplotlib.pyplot as plt
import numpy as np

from error_level import latex_table

def replace_headers(_models):
    new_headers = []
    for header in _models:
        if header in llm_mapping:
            new_headers.append(llm_mapping[header])
        else:
            new_headers.append(header)
    return new_headers

# Path to 104 api diff
api_diff_path = '/Users/frank/Documents/Work/PHD/bacardi/bacardi/analysis/api_diff.txt'

root_folder_path = '/Users/frank/Documents/Work/PHD/bacardi/bacardi/results'

prompt_aliases = {
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
    "deepseek-deepseek-chat": "\\textbf{Deepseek V3}",
    "gemini-2.0-flash-001": "\\textbf{Gemini-2.0-flash}",
    "gpt-4o-mini": "\\textbf{Gpt-4o-mini}",
    "o3-mini-2025-01-31": "\\textbf{o3-mini}",
    "qwen-qwen2.5-32b-instruct": "\\textbf{Qwen2.5-32b-instruct}"
}

# Read the lines from the text file
with open(api_diff_path, 'r') as file:
    lines = [line.strip() for line in file.readlines()]

## print all results
print(f' Number of lines {len(lines)}')

# Dictionary to store the results
results = {}
files_fixed = {}

# Iterate over the prompts in the root folder
for prompt_folder in os.listdir(root_folder_path):
    prompt_path = os.path.join(root_folder_path, prompt_folder)
    if os.path.isdir(prompt_path):
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
                        total_prefix_files = 0
                        total_fixed_files = 0
                        total_files = 0
                        total_commits = 0
                        for commit, details in data.items():
                            if commit in lines:
                                total_commits += 1
                                for attempt in details['attempts']:
                                    if attempt['failureCategory'] == "ERROR_MODEL_RESPONSE" or attempt[
                                        'failureCategory'] == "NOT_REPAIRED":
                                        total_prefix_files += attempt['prefixFiles']
                                        total_fixed_files += 0
                                    else:
                                        total_prefix_files += attempt['prefixFiles']
                                        total_fixed_files += attempt['fixedFiles']
                                        # total_files += attempt['prefixFiles'] + attempt['postfixFiles']
                                    total_files += total_prefix_files
                        print(f'Commits: {total_commits}')
                        # Calculate the percentage of fixed files
                        if total_files > 0:
                            fixed_percentage = (total_fixed_files / total_prefix_files) * 100
                        else:
                            fixed_percentage = 0

                        # Store the results for each model
                        prompt_results[model_folder] = {
                            'total_prefix_files': total_prefix_files,
                            'total_fixed_files': total_fixed_files,
                            'fixed_percentage': fixed_percentage
                        }

        results[prompt_folder] = prompt_results

# Print the results
for prompt, models in results.items():
    print(f"Prompt: {prompt}")
    for model, result in models.items():
        print(f"  Model: {model}")
        print(f"    Total Prefix Files: {result['total_prefix_files']}")
        print(f"    Total Fixed Files: {result['total_fixed_files']}")
        print(f"    Fixed Percentage: {result['fixed_percentage']:.2f}%")
    print()

# Prepare data for LaTeX table
table_data = []
prompts = sorted(results.keys(), key=lambda x: prompt_aliases.get(x, x))
headers = ["\\textbf{Prompt}"] + sorted(next(iter(results.values())).keys())



for prompt in prompts:
    row = [prompt_aliases.get(prompt, prompt)]
    for model in sorted(next(iter(results.values())).keys()):
        result = results[prompt][model]
        fixed_percentage = result['fixed_percentage']
        total_fixed_files = result['total_fixed_files']
        total_prefix_files = result['total_prefix_files']
        row.append(f"{total_fixed_files}/{total_prefix_files} ({fixed_percentage:.2f}\\%)")
    table_data.append(row)

# Generate LaTeX table without highlighted maximum values
latex_table = "\\newcommand{\\fileerrorleveltab}{%\n"
latex_table += "\\centering\n"
latex_table +="\\rowcolors{2}{gray!10}{white}\n"
latex_table += "\\caption{Effectiveness of \\toolname in Fixing Compilation Failures at the File Level}\n"
latex_table += "\\label{tab:file_error_level}\n"
latex_table += "\\begin{tabular}{l" + "r" * len(headers[1:]) + "}\n"
latex_table += "\\toprule\n"
latex_table += " & ".join(replace_headers(headers)) + " \\\\\n"
latex_table += "\\hline\n"
for row in table_data:
    latex_table += " & ".join(row) + " \\\\\n"
latex_table += "\\end{tabular}"

print(latex_table)
output_json_path = 'fixed_error_rate.json'

with open(output_json_path, 'w') as json_file:
    json.dump(results, json_file, indent=4)


# Create a file with all the results stored as \pgfkeys commands
output_pgfkeys_path = 'results_pgfkeys.tex'

with open(output_pgfkeys_path, 'w') as pgfkeys_file:
    for prompt, models in results.items():
        for model, result in models.items():
            total_fixed_files = result['total_fixed_files']
            total_prefix_files = result['total_prefix_files']
            fixed_percentage = result['fixed_percentage']
            key_base = f"{model.replace('-', '_')}_{prompt_aliases[prompt].strip('$')}_BUILD_SUCCESS"
            pgfkeys_file.write(f"\\pgfkeyssetvalue{{{key_base}}}{{{total_fixed_files}/{total_prefix_files}}}\n")
            pgfkeys_file.write(f"\\pgfkeyssetvalue{{{key_base}_percent}}{{{fixed_percentage:.2f}}}\n")

# Generate the LaTeX table using these keys
latex_table_pgfkeys = "\\newcommand{\\fileerrorleveltab}{%\n"
latex_table_pgfkeys += "\\centering\n"
latex_table_pgfkeys += "\\rowcolors{2}{gray!10}{white}\n"
latex_table_pgfkeys += "\\caption{Effectiveness of \\toolname in Fixing Compilation Failures at the File Level}\n"
latex_table_pgfkeys += "\\label{tab:file_error_level}\n"
latex_table_pgfkeys += "\\begin{tabular}{l" + "r" * len(headers[1:]) + "}\n"
latex_table_pgfkeys += "\\toprule\n"
latex_table_pgfkeys += " & ".join(replace_headers(headers)) + " \\\\\n"
latex_table_pgfkeys += "\\hline\n"
for prompt in prompts:
    row = [prompt_aliases.get(prompt, prompt)]
    for model in sorted(next(iter(results.values())).keys()):
        key_base = f"{model.replace('-', '_')}_{prompt_aliases[prompt].strip('$')}_BUILD_SUCCESS"
        row.append(f"\\pgfkeysvalueof{{{key_base}}}(\\pgfkeysvalueof{{{key_base}_percent}}\\%)")
    latex_table_pgfkeys += " & ".join(row) + " \\\\\n"
latex_table_pgfkeys += "\\end{tabular}\n"
latex_table_pgfkeys += "}"

print(latex_table_pgfkeys)

output_json_path = 'fixed_error_rate.json'

with open(output_json_path, 'w') as json_file:
    json.dump(results, json_file, indent=4)





