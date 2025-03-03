import json
from os import write


def extract_breaking_commits(_json_file_path):
    with open(_json_file_path, 'r') as json_file:
        data = json.load(json_file)

    _breaking_commits = {}
    for commit, commit_data in data.items():
        failure_category = commit_data.get('attempts', [{}])[0].get('failureCategory', 'N/A')
        if failure_category != 'NOT_APIDIFF':
            _breaking_commits[commit] = failure_category

    return _breaking_commits


# Example usage
json_file_path = '/Users/frank/Documents/Work/PHD/bacardi/bacardi/results/results_baseline_api_diff/gpt-4o-mini/result_repair_gpt-4o-mini.json'
breaking_commits = extract_breaking_commits(json_file_path)

# write result into txt, one commit per line
output_file = 'apu_diff_breaking_commits.txt'
with open(output_file, 'w') as f:
    for commit in breaking_commits:
        f.write(f'{commit}\n')
