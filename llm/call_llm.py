from openai import OpenAI

def get_llm_response(prompt, api_key, organization, temperature=0.7, output_file="response.txt"):
    client = OpenAI(
        organization=organization,
        api_key=api_key
    )

    stream = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[{"role": "user", "content": prompt}],
        stream=True,
    )

    response_text = ""
    with open(output_file, "w") as file:
        for chunk in stream:
            if chunk.choices[0].delta.content is not None:
                file.write(chunk.choices[0].delta.content)
                response_text += chunk.choices[0].delta.content

    return response_text

def check_openai_installation():
    try:
        print(f"OpenAI package version: {openai.__version__}")
        print("OpenAI package is installed correctly.")
    except ImportError:
        print("OpenAI package is not installed.")
    except Exception as e:
        print(f"An error occurred: {e}")

if __name__ == "__main__":
    api_key = "sk-None-APHBjbapVsZJI436GiokT3BlbkFJjhWT9WJcegltDZt56m3p"
    organization = "org-8vaaikANoGLw18qMPf7FeuJm"
    prompt="""Act as an Automatic Program Repair (APR) tool, reply only with code, without explanation.
    You are specialized in breaking dependency updates, in which the failure is caused by an external dependency.
    To solve the failure you can only work on the client code.
    
    propose a patch that I can apply to the code in order to fix the error.
    Your response will be automatically parsed by an algorithm, be sure to only return the code.
    be sure to return a code segment that can replace the entire failing client code.
    you CANNOT change the function signature, but you can create variables if they help to make the code easier to understand.
    you CAN remove the Override decorator IF AND ONLY IF the method is not overriding any method in the new version.
    you CANNOT use removed library methods identified with `--`, but you CAN use added library methods identified with `++`.
    DO NOT USE '--' or '++' to indicate changes you make on the code, be sure to return only code that can be compiled.
    return only the fixed failing method, not the complete class code, you MUST change something in the code.
    
    the following client code fails:
    ```java
    private void prepareTextfieldWithBorder(final JRDesignTextElement textField, final ColumnStyle style)
        {
            final ColumnBorder border = style.getColBorder();
            if(border == null)
            {
                return;
            }
            
            textField.getLineBox().getPen().setLineWidth(border.getLineWidth());
            textField.getLineBox().getPen().setLineColor(border.getLineColor());
            textField.getLineBox().getPen().setLineStyle(border.getLineStyle().getLineStyleEnum());
        }
    ```
    
    with the following error message:
    [ERROR] /biapi/src/main/java/xdev/tableexport/export/ReportBuilder.java:[369,81] incompatible types: int cannot be converted to java.lang.Float
    
    
    the new library version includes the following changes, where `--` indicates a removal and `++` indicates an addition:
    -- public abstract void net.sf.jasperreports.engine.JRPen.setLineWidth(float)
    -- public void net.sf.jasperreports.engine.base.JRBasePen.setLineWidth(float)
    
    you also know that the failing method is inserted in this class:
    ```java
    /**
     * The {@link ReportBuilder} generates a {@link JasperReport} based on the
     * information of the {@link TemplateConfig}.
     *
     * @author XDEV Software (FHAE)
     * @see TemplateConfig
     * @see ReportExporter
     */
    public class ReportBuilder {
        private final TemplateConfig config;
    
        private final Set<JRDesignField> fieldSet = new HashSet<>();
    
        public ReportBuilder(final TemplateConfig tempConfig) {
            this.config = tempConfig;
        }
    
        private void prepareTextfieldWithBorder(final JRDesignTextElement textField, final ColumnStyle style) {
            final ColumnBorder border = style.getColBorder();
            if (border == null) {
                return;
            }
            textField.getLineBox().getPen().setLineWidth(border.getLineWidth());
            textField.getLineBox().getPen().setLineColor(border.getLineColor());
            textField.getLineBox().getPen().setLineStyle(border.getLineStyle().getLineStyleEnum());
        }
    }
    ```"""
    response = get_llm_response(prompt, api_key, organization, temperature=0.5)
    print(response)
