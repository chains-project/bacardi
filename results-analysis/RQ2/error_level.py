import json
import os
from tabulate import tabulate
import matplotlib.pyplot as plt
import numpy as np

# Path to 1682/1910 (88.06\%)
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
                        total_prefix_errors = 0
                        total_fixed_errors = 0
                        total_errors = 0
                        total_commits = 0
                        for commit, details in data.items():
                            if commit in lines:
                                total_commits += 1
                                for attempt in details['attempts']:
                                    if attempt['failureCategory'] == "ERROR_MODEL_RESPONSE":
                                        total_prefix_errors += attempt['prefixErrors']
                                        total_fixed_errors += 0
                                    else:
                                        total_prefix_errors += attempt['prefixErrors']
                                        total_fixed_errors += attempt['fixedErrors']
                                        # total_files += attempt['prefixFiles'] + attempt['postfixFiles']
                                    total_errors += total_prefix_errors
                        print(f'Commits: {total_commits}')
                        # Calculate the percentage of fixed files
                        if total_errors > 0:
                            fixed_percentage = (total_fixed_errors / total_prefix_errors) * 100
                        else:
                            fixed_percentage = 0

                        # Store the results for each model
                        prompt_results[model_folder] = {
                            'total_prefix_errors': total_prefix_errors,
                            'total_fixed_errors': total_fixed_errors,
                            'fixed_percentage': fixed_percentage
                        }

        results[prompt_folder] = prompt_results

# Print the results
for prompt, models in results.items():
    print(f"Prompt: {prompt}")
    for model, result in models.items():
        print(f"  Model: {model}")
        print(f"    Total Prefix Errors: {result['total_prefix_errors']}")
        print(f"    Total Fixed Errors: {result['total_fixed_errors']}")
        print(f"    Fixed Percentage: {result['fixed_percentage']:.0f}%")
    print()

# Prepare data for LaTeX table
table_data = []
prompts = sorted(results.keys(), key=lambda x: prompt_aliases.get(x, x))
headers = ["Prompt"] + sorted(next(iter(results.values())).keys())

for prompt in prompts:
    row = [prompt_aliases.get(prompt, prompt)]
    for model in sorted(next(iter(results.values())).keys()):
        result = results[prompt][model]
        fixed_percentage = result['fixed_percentage']
        total_fixed_errors = result['total_fixed_errors']
        total_prefix_errors = result['total_prefix_errors']
        row.append(f"{total_fixed_errors}/{total_prefix_errors} ({fixed_percentage:.0f}\\%)")
    table_data.append(row)

# Generate LaTeX table without highlighted maximum values
latex_table = "\\begin{tabular}{l" + "r" * len(headers[1:]) + "}\n"
latex_table += " & ".join(headers) + " \\\\\n"
latex_table += "\\hline\n"
for row in table_data:
    latex_table += " & ".join(row) + " \\\\\n"
latex_table += "\\end{tabular}"

print(latex_table)
output_json_path = 'fixed_error_rate.json'
with open(output_json_path, 'w') as json_file:
    json.dump(results, json_file, indent=4)



# Ordenar prompts y modelos
prompts = sorted(results.keys(), key=lambda x: prompt_aliases.get(x, x))
models = sorted(next(iter(results.values())).keys())

# Obtener los valores en un diccionario con listas
data = {model: [] for model in models}
max_values_per_prompt = []

for prompt in prompts:
    values = [(model, results[prompt][model]['fixed_percentage']) for model in models]
    values.sort(key=lambda x: x[1])  # Ordenar de menor a mayor porcentaje

    max_value = max(val[1] for val in values)  # Máximo % en el prompt
    max_values_per_prompt.append(max_value)

    bottom = 0  # Base acumulativa
    for model, value in values:
        data[model].append(value - bottom)  # Porcentaje exacto del modelo en la barra
        bottom = value  # Actualizar la base

# Configurar el gráfico
fig, ax = plt.subplots(figsize=(10, 6))
index = np.arange(len(prompts))

# Colores para los modelos
colors = plt.cm.viridis(np.linspace(0.2, 0.8, len(models)))

# Graficar las barras apiladas
bottom = np.zeros(len(prompts))
for i, model in enumerate(models):
    bars = ax.barh(index, data[model], height=0.6, label=model, left=bottom, color=colors[i])

    # Agregar etiquetas con el porcentaje real dentro de la barra
    for bar, value in zip(bars, data[model]):
        if value > 0:  # Solo mostrar si el valor es mayor a 0
            x_position = bar.get_x() + bar.get_width() / 2  # Centro de la barra
            y_position = bar.get_y() + bar.get_height() / 2  # Centro en Y
            ax.text(x_position, y_position, f"{results[prompt][model]['fixed_percentage']:.1f}%", ha='center',
                    va='center', fontsize=10, color='black')
    bottom += np.array(data[model])  # Acumular para la siguiente capa de la barra

# Configurar etiquetas y diseño
ax.set_xlabel('Fixed Percentage')
ax.set_title('Fixed Percentage by Model and Prompt')

# Asignar los alias en el eje Y correctamente
alias_labels = [prompt_aliases[prompt] for prompt in prompts]
# ax.set_yticks(alias_labels)
ax.set_yticklabels(alias_labels)

ax.legend(title="Models")

plt.xlim(0, max(max_values_per_prompt) * 1.1)  # Ajustar el límite del eje X
plt.tight_layout()
# plt.show()