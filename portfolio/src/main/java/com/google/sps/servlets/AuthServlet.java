package com.google.sps.servlets;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
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

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    if(!userService.isUserLoggedIn()) {
      return;
    }

    String email = userService.getCurrentUser().getEmail();
    String nickname = request.getParameter("nickname");
    // update user info in datastore
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    Entity userInfo = new Entity("User", email);
    userInfo.setProperty("name", nickname);
    userInfo.setProperty("email", email);
    datastore.put(userInfo);

    // update nickname for all of user's comments
    Query query =
        new Query("Comment")
            .setFilter(new Query.FilterPredicate("author", Query.FilterOperator.EQUAL, email));
    PreparedQuery results = datastore.prepare(query);
    for(Entity e: results.asIterable()) {
      e.setProperty("name", nickname);
      datastore.put(e);
    }
    response.sendRedirect("/index.html");
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