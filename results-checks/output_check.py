import argparse
import json
import logging
import os


def setup_logging(result_file):
    log_file = os.path.splitext(result_file)[0] + '.log'
    logging.basicConfig(filename=log_file, level=logging.INFO, 
                        format='%(asctime)s - %(levelname)s - %(message)s')

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
                fixed_files = attempt.get('fixedFiles', 0)  # Assuming fixedFiles might be missing
                prefix_files = attempt.get('prefixFiles', 0)  # Assuming prefixFiles might be missing
                postfix_errors = attempt['postfixErrors']
                unfixed_errors = attempt['unfixedErrors']
                new_errors = attempt['newErrors']
                fixed_errors = attempt.get('fixedErrors', 0)  # Assuming fixedErrors might be missing
                prefix_errors = attempt.get('prefixErrors', 0)  # Assuming prefixErrors might be missing

                # Check if postfixFiles is equal to the sum of unfixedFiles and newFiles
                if postfix_files != (unfixed_files + new_files):
                    mismatches.append(commit)
                    print(f"Mismatch in files for commit {commit}: postfixFiles={postfix_files}, unfixedFiles+newFiles={unfixed_files + new_files}, unfixedFiles = {unfixed_files}, newFiles = {new_files}")
                    logging.info(f"Mismatch in files for commit {commit}: postfixFiles={postfix_files}, unfixedFiles+newFiles={unfixed_files + new_files}, unfixedFiles = {unfixed_files}, newFiles = {new_files}")

                # Check if postfixErrors is equal to the sum of unfixedErrors and newErrors
                if postfix_errors != (unfixed_errors + new_errors):
                    mismatches.append(commit)
                    print(f"Mismatch in errors for commit {commit}: postfixErrors={postfix_errors}, unfixedErrors+newErrors={unfixed_errors + new_errors}, unfixedErrors = {unfixed_errors}, newErrors = {new_errors}")
                    logging.info(f"Mismatch in errors for commit {commit}: postfixErrors={postfix_errors}, unfixedErrors+newErrors={unfixed_errors + new_errors}, unfixedErrors = {unfixed_errors}, newErrors = {new_errors}")

                # Check if prefixFiles is equal to the sum of unfixedFiles and fixedFiles
                if prefix_files != (unfixed_files + fixed_files):
                    mismatches.append(commit)
                    print(f"Mismatch in files for commit {commit}: prefixFiles={prefix_files}, unfixedFiles+fixedFiles={unfixed_files + fixed_files}, unfixedFiles = {unfixed_files}, fixedFiles = {fixed_files}")
                    logging.info(f"Mismatch in files for commit {commit}: prefixFiles={prefix_files}, unfixedFiles+fixedFiles={unfixed_files + fixed_files}, unfixedFiles = {unfixed_files}, fixedFiles = {fixed_files}")

                # Check if prefixErrors is equal to the sum of unfixedErrors and fixedErrors
                if prefix_errors != (unfixed_errors + fixed_errors):
                    mismatches.append(commit)
                    print(f"Mismatch in errors for commit {commit}: prefixErrors={prefix_errors}, unfixedErrors+fixedErrors={unfixed_errors + fixed_errors}, unfixedErrors = {unfixed_errors}, fixedErrors = {fixed_errors}")
                    logging.info(f"Mismatch in errors for commit {commit}: prefixErrors={prefix_errors}, unfixedErrors+fixedErrors={unfixed_errors + fixed_errors}, unfixedErrors = {unfixed_errors}, fixedErrors = {fixed_errors}")

    return mismatches                

def parse_arguments():
    parser = argparse.ArgumentParser(description='Process JSON files to run Docker commands via SSH.')
    parser.add_argument('--outputfile', required=True, type=str, help='JSON output file path.')
    parser.add_argument('--resultfile', required=True, type=str, help='Name to store the checks output.')
    return parser.parse_args()


def main():
    args = parse_arguments()
    setup_logging(args.resultfile)
    mismatches=check_output(args.outputfile)
    if len(mismatches)==0:
        logging.info("Output results checked out correct!")
    else:
        logging.info(f"{len(mismatches)} mismatches found.")
          # Write mismatches to the result file
        with open(args.resultfile, 'w') as file:
            json.dump(mismatches, file, indent=4)

if __name__ == "__main__":
    main()