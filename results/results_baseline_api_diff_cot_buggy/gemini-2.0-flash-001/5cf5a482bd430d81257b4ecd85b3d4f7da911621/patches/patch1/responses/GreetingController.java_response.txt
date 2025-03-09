import javax.mvc.Models;
import javax.mvc.UriRef;
import javax.mvc.binding.BindingResult;
import javax.mvc.binding.MvcBinding;
import javax.mvc.binding.ParamError;
import javax.mvc.security.CsrfProtected;
import javax.mvc.Controller;

Models models;
BindingResult bindingResult;
@UriRef("greeting-post")
@MvcBinding
@Controller