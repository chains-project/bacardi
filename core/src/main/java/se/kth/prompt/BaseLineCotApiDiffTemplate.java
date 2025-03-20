package se.kth.prompt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseLineCotApiDiffTemplate extends BasePromptApiDiffTemplate {

    private static final Logger log = LoggerFactory.getLogger(BaseLineCotApiDiffTemplate.class);


    @Override
    public String generatePrompt() {

        return """
                %s
                
                %s
                
                %s
                
                Before proposing a fix, please analyze the situation and plan your approach within <repair_strategy> tags:
                
                 1. Identify the specific API changes that are causing the failure in the client code.
                 2. Compare the old and new API versions, noting any changes in method signatures, return types, or parameter lists.
                 3. Determine which parts of the client code need to be updated to accommodate these API changes.
                 4. Consider any constraints or requirements for the fix (e.g., not changing function signatures, potential import adjustments).
                 5. Plan the minimal set of changes needed to fix the issue while keeping the code functional and compliant with the new API.
                 6. Consider potential side effects of the proposed changes on other parts of the code.
                 7. Ensure that the planned changes will result in a complete and compilable class.
                 8. If applicable, note any additional imports that may be needed due to the API changes.
                
                propose a patch that can be applied to the code to fix the issue.
                Return only a complete and compilable class in a fenced code block.
                Do not remove any code that you don't want to update keep it in the code block. Do not use "// ... (rest of the code remains unchanged)" in your response.
                You CANNOT change the function signature of any method but may create variables if it simplifies the code.
                You CAN remove the @Override annotation IF AND ONLY IF the method no longer overrides a method in the updated dependency version.
                If fixing the issue requires addressing missing imports, ensure the correct package or class is used in accordance with the newer dependency version.
                Avoid removing any existing code unless it directly causes a compilation or functionality error. Don't use the comment "// ... (rest of the class remains unchanged)".
                Return only the fixed class, ensuring it fully compiles and adheres to these constraints.
                """.formatted(header().stripTrailing(), classCode().stripTrailing(), errorLog().stripTrailing());
    }


}
