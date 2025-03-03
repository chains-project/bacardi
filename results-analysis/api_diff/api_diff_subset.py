import os
import csv
import json
import pandas as pd

def convert_txt_to_csv(input_txt, input_dir, output_csv):
    with open(input_txt, 'r') as txt_file:
        commits = [line.strip() for line in txt_file.readlines()]

    level_one_subdirs = [d for d in os.listdir(input_dir) if os.path.isdir(os.path.join(input_dir, d))]
    subdirs = {}
    json_names = {}
    for subdir in level_one_subdirs:
        subdirs[subdir] = [d for d in os.listdir(os.path.join(input_dir, subdir)) if os.path.isdir(os.path.join(input_dir, subdir, d))]
        json_names[subdir] = {}
        for subsubdir in subdirs[subdir]:
            subsubdir_path = os.path.join(input_dir, subdir, subsubdir)
            json_files = [f for f in os.listdir(subsubdir_path) if f.endswith('.json')]
            if json_files:
                json_name = json_files[0].split('result_repair_')[1].split('-')[0]
                json_names[subdir][subsubdir] = json_name

    data = []
    headers = ['Commit']
    for subdir in level_one_subdirs:
        for subsubdir in subdirs[subdir]:
            json_name = json_names[subdir].get(subsubdir, '')
            headers.append(f'{subdir} - {json_name}')

    for commit in commits:
        row = [commit]
        for subdir in level_one_subdirs:
            for subsubdir in subdirs[subdir]:
                subsubdir_path = os.path.join(input_dir, subdir, subsubdir)
                json_files = [f for f in os.listdir(subsubdir_path) if f.endswith('.json')]
                if json_files:
                    json_file_path = os.path.join(subsubdir_path, json_files[0])
                    with open(json_file_path, 'r') as json_file:
                        data_json = json.load(json_file)
                        commit_data = data_json.get(commit, {})
                        failure_category = commit_data.get('attempts', [{}])[0].get('failureCategory', 'N/A')
                        row.append(failure_category)
                else:
                    row.append('N/A')
        data.append(row)

    df = pd.DataFrame(data, columns=headers)
    df.to_csv(output_csv, index=False)

    # Group by model and count categories
    grouped_data = df.melt(id_vars=['Commit'], var_name='Model', value_name='Category')
    category_counts = grouped_data.groupby(['Model', 'Category']).size().unstack(fill_value=0)

    # Write to Excel with multiple sheets
    with pd.ExcelWriter(output_csv.replace('.csv', '.xlsx')) as writer:
        df.to_excel(writer, sheet_name='Commits', index=False)
        category_counts.to_excel(writer, sheet_name='Category Counts')

    print(f'File {output_csv.replace(".csv", ".xlsx")} generated successfully.')

# Paths of the input text file, input directory, and output CSV file
input_txt = '/Users/frank/Documents/Work/PHD/bacardi/bacardi/scripts/results/api_diff/apu_diff_breaking_commits.txt'
input_dir = '/Users/frank/Documents/Work/PHD/bacardi/bacardi/results'
output_csv = 'commits_with_values.csv'

# Convert the TXT file and directory to a CSV file
convert_txt_to_csv(input_txt, input_dir, output_csv)