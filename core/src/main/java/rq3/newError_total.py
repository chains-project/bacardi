import json

def calculate_total_new_errors(json_path):
    total_new_errors = 0

    with open(json_path, 'r') as file:
        data = json.load(file)

        for commit in data.values():
            for attempt in commit['attempts']:
                total_new_errors += attempt.get('newErrors', 0)

    return total_new_errors

if __name__ == "__main__":
    json_file = "/Users/frank/Documents/Work/PHD/bacardi/bacardi/results/results_baseline_buggy_line_api_diff/gpt-4o-mini/result_repair_gpt-4o-mini.json"
    total = calculate_total_new_errors(json_file)
    print(f"Total de newErrors: {total}")