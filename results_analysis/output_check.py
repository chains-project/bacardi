import argparse
import json


def check_output(output_file):
    mismatches=[]
    # Load the JSON file
    with open(output_file) as file:
        data = json.load(file)

        # Iterate through each entry in the JSON file
        for commit, details in data.items():
            for attempt in details['attempts']:
                postfix_files = attempt['postfixFiles']
                unfixed_files = attempt['unfixedFiles']
                new_files = attempt['newFiles']
                postfix_errors = attempt['postfixErrors']
                unfixed_errors = attempt['unfixedErrors']
                new_errors = attempt['newErrors']

                # Check if postfixFiles is equal to the sum of unfixedFiles and newFiles
                if postfix_files != (unfixed_files + new_files):
                    mismatches.append(commit)
                    print(f"Mismatch in files for commit {commit}: postfixFiles={postfix_files}, unfixedFiles+newFiles={unfixed_files + new_files}")

                # Check if postfixErrors is equal to the sum of unfixedErrors and newErrors
                if postfix_errors != (unfixed_errors + new_errors):
                    mismatches.append(commit)
                    print(f"Mismatch in errors for commit {commit}: postfixErrors={postfix_errors}, unfixedErrors+newErrors={unfixed_errors + new_errors}")
    return mismatches                

def parse_arguments():
    parser = argparse.ArgumentParser(description='Process JSON files to run Docker commands via SSH.')
    parser.add_argument('--outputfile', required=True, type=str, help='JSON output file path.')
    parser.add_argument('--resultfile', required=True, type=str, help='Name to store the checks output.')
    return parser.parse_args()


def main():
    args = parse_arguments()
    mismatches=check_output(args.outputfile)
    if len(mismatches)==0:
        print("Output results checked out correct!")
    else:
        print(f"{len(mismatches)} mismatches found.")
          # Write mismatches to the result file
        with open(args.resultfile, 'w') as file:
            json.dump(mismatches, file, indent=4)

if __name__ == "__main__":
    main()