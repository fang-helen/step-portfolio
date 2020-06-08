package com.google.sps.servlets;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;

@WebServlet("/auth")
public class AuthServlet extends HttpServlet {
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    Gson gson = new Gson();
    LoginObject info;

    UserService userService = UserServiceFactory.getUserService();
    if (userService.isUserLoggedIn()) {
      String userEmail = userService.getCurrentUser().getEmail();
      String logoutUrl = userService.createLogoutURL("/");
      info = new LoginObject(logoutUrl, userEmail);

    } else {
      String loginUrl = userService.createLoginURL("/");
      info = new LoginObject(loginUrl, null);

    }
    response.getWriter().println(gson.toJson(info));
  }

  /* nested class to help with JSON conversion */
  private static class LoginObject {
    boolean loggedIn;
    String url;
    String email;

    public LoginObject(String url, String email) {
      this.url = url;
      this.email = email;
      loggedIn = (email != null);
    }
  }
}