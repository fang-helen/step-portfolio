package com.google.sps.servlets;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;

@WebServlet("/auth")
public class AuthServlet extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(AuthServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    Gson gson = new Gson();
    LoginObject info;

    UserService userService = UserServiceFactory.getUserService();
    if (userService.isUserLoggedIn()) {
      String userEmail = userService.getCurrentUser().getEmail();
      String logoutUrl = userService.createLogoutURL("/");

      DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
      Query query =
        new Query("User")
            .setFilter(new Query.FilterPredicate("email", Query.FilterOperator.EQUAL, userEmail));
      String userNickname = userEmail;
      PreparedQuery results = datastore.prepare(query);
      Entity entity = results.asSingleEntity();
      if (entity != null) {
        userNickname = entity.getProperty("name").toString();
      }
      info = new LoginObject(logoutUrl, userEmail, userNickname);
      LOGGER.info("currently logged in to account " + userEmail + ". Created logout URL " + logoutUrl);
    } else {
      String loginUrl = userService.createLoginURL("/");
      info = new LoginObject(loginUrl, null, null);
      LOGGER.info("not currently logged in. Created login URL " + loginUrl);
    }
    response.getWriter().println(gson.toJson(info));
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    UserService userService = UserServiceFactory.getUserService();
    if(!userService.isUserLoggedIn()) {
      LOGGER.info("User not currently logged in, returning.");
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
    LOGGER.info("Updated nickname to " + nickname + " for user " + email);
    response.sendRedirect("/index.html");
  }

  /* nested class to help with JSON conversion */
  private static class LoginObject {
    boolean loggedIn;
    String url;
    String email;
    String name;

    public LoginObject(String url, String email, String name) {
      this.url = url;
      this.email = email;
      this.name = name;
      loggedIn = (email != null);
    }
  }
}
