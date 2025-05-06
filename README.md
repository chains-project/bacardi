# bacardi
Fix breaking dependency updates

## Repository Contents

- **[classifier](breaking-classifier)**: classify breaking dependency updates and extract information from the logs
- **[docker-build](docker-build)**: build docker images to reproduce the breaking dependency updates and patches to fix them
- **[core](core)**: core functionality to fix breaking dependency updates
- **[git-manager](git-manager)**: manage git repositories and create new branches for each failure category
- **[extractor](extractor)**: extract information from the client's repository and api diff

## Setup and Installation

To replicate the experiments, you need to set up the environment and build the project. Follow these steps:

1. Clone the repository:
   ```bash
   git clone git@github.com:chains-project/bacardi.git
   cd bacardi
    ```
2. Build the project:
   ```bash
   mvn clean install
   ```
3. Setup environment variable:
   ```bash
   cp .env.example .env
   ```

## Usage

Run the application using the following command:
```bash
    java -jar ./core/target/Bump.jar
```

To execute a single fix specify in env file:
```
    SPECIFIC_FILE=<commit_hash>
```
---
## Results
The results of the experiments are stored in the `results` directory. Each experiment is organized into subdirectories, containing the following files:
- `results_<prompt_name>`: Contains the results of the experiment for a specific prompt.
- `<model_n>`: Contains the results for a specific model.
```
📁 results
├── 📁 results_<prompt_name> each prompt results
│   ├── 📁 <model_n> model results
│   ├── -----
│   ├── 📁 <model_n> model results
```

---

## License

This project is licensed under the [MIT License](LICENSE).

---

Thank you for using **Bacardi**! If you encounter any issues or have feedback, feel free to create an [issue](https://github.com/chains-project/bacardi/issues).