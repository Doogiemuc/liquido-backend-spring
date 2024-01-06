package org.doogie.liquido.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@RestController
@RequestMapping("/error")
public class LiquidoErrorController implements ErrorController {
	@Override
	public String getErrorPath() {
		return "/error";
	}

	@RequestMapping(
		produces = {"text/html"}
	)
	public String errorHtml(HttpServletRequest request, HttpServletResponse response) {
		log.info("ERROR: " + request.getMethod() + " " + request.getRequestURI() + " => Response Status: " + response.getStatus());
		return
			"<!DOCTYPE html>\n" +
			"<html>\n" +
			"<body>\n" +
			"<h1>LIQUIDO</h1>\n" +
			"<p><a href=\"www.liquido.vote\">www.liquido.vote</a> - the free, secure and liquid eVoteing app.</p>\n" +
			"<p>This is the LIQUIDO Backend. If you see this then the backend is up and running.</p>\n" +
			"<p>The API you are most likely looking for is available under <pre>/liquido-api/v3</pre></p>\n" +
			"<p>Many greetings, the LIQUIDO team</p>\n" +
				"<hr><pre>\n" +
				"<h3>Response</h3>\n" +
				"Status: " + response.getStatus() + "\n" +
				"</pre>\n" +
			"</body>\n" +
			"</html>\n";
	}


}
