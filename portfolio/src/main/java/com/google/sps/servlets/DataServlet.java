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

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private ArrayList<String> msgs;

  @Override
  public void init() {
    msgs = new ArrayList<>();

    msgs.add("Hello!");
    msgs.add("Here is a message.");
    msgs.add("Here is another message.");
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html;");
    String json = convertToJson(msgs);
    response.getWriter().println(json);
  }

  private String convertToJson(ArrayList<String> list) {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      sb.append("\"name\":\"msgs\",");
      sb.append("\"contents\":");
      sb.append("[");
      for(int i = 0; i < msgs.size()-1; i++) {
        sb.append("{\"message\": ");
        sb.append("\"");
        sb.append(msgs.get(i));
        sb.append("\"},");
      }
      sb.append("{\"message\": ");
      sb.append("\"");
      sb.append(msgs.get(msgs.size()-1));
      sb.append("\"}");

      sb.append("]");
      sb.append("}");

      return sb.toString();

  }
}
