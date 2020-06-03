// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.gson.Gson;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private List<Entity> database;
  private int limit;
  private boolean descending;
  private String sortParam;

  @Override
  public void init() {
    database = new ArrayList<>();
    limit = 10;
    sortParam = "timestamp";
    descending = true;
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    
    // deleting single comment
    if(request.getParameter("index") != null) {
      int i = Integer.parseInt(request.getParameter("index"));
      if(i > database.size()) {
        throw new IOException("requested index is out of bounds");
      }
      datastore.delete(database.get(i).getKey());
      database.remove(i);
      return;
    }

    // querying datastore elements
    String paramLimit = request.getParameter("limit");
    String paramSort = request.getParameter("sort");
    String paramChoice = request.getParameter("sortBy");
    if(paramLimit != null) {
      limit = Integer.parseInt(paramLimit);
    }
    if(paramSort != null) {
        if(paramSort.equals("descending")) {
            descending = true;
        }
        else if (paramSort.equals("ascending")) {
            descending = false;
        }
    }
    if(paramChoice != null) {
        sortParam = paramChoice;
    }
    // todo: only recompute if new parameters don't match old ones. else save json string?
    Query query = new Query("Comment").addSort(sortParam, (descending) ? SortDirection.DESCENDING : SortDirection.ASCENDING);
    PreparedQuery results = datastore.prepare(query);

    database = new ArrayList<>();
    Iterable<Entity> resultIt = results.asIterable();
    Iterator<Entity> itr = resultIt.iterator();
    for(int i = 0; i < limit && itr.hasNext(); i++) {
        database.add(itr.next());
    }

    response.setContentType("application/json;");
    Gson gson = new Gson();
    String json = gson.toJson(database);
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String text = request.getParameter("enter-text");
    if(text == null || text.length() == 0) {
        response.sendRedirect("/index.html");
        return;
    }

    Entity comment = new Entity("Comment");
    comment.setProperty("content", text);
    comment.setProperty("timestamp", System.currentTimeMillis());

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(comment);

    response.sendRedirect("/index.html");
  }
}
