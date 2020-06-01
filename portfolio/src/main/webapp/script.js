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


/**
 * Adjusts layout of page at load time based on window size.
 * If the window is narrow, displayes items in a single column instead of side-by-side
 */
 function loadLayout() {
     if (window.innerWidth < 1000) {
         var body = document.getElementById("body");
         body.style.display = "block";

         var left = document.getElementById("content-left");
         left.style.margin = "auto";
         left.style.padding = "0";

         var right = document.getElementById("content-right");
         right.style.border = "none";
         right.style.margin = "auto";
         right.style.padding = "0";
     }
 }

/**
 * Adds a random greeting to the page.
 */
function addRandomGreeting() {
  const greetings = [
      'I speak fluent Mandarin and some Spanish.',
      'I really like Chinese dramas.',
      'I\'ve sewn a lot of pencil bags, but don\'t use most of them.',
      'I have a younger brother at home. We\'re 9 years apart.',
      'I think pineapple on pizza is not bad.',
      'I might double major in math.',
      'I was born in Utah and spent 5 years in Oregon before moving to Texas.',
    ];

  // Pick a random greeting.
  const greeting = greetings[Math.floor(Math.random() * greetings.length)];

  // Add it to the page.
  const greetingContainer = document.getElementById('greeting-container');
  greetingContainer.innerText = greeting;
}

/**
 * Applies a rotation effect to the clicked icon and toggles the drop-down.
 * If the icon is a "+", it is rotated into an "x" and the drop-down contents are displayed.
 * If the icon is an "x", it is rotated back into a "+" and the drop-down is collapsed.
 */
function rotateItem(index) {
    var icon = document.getElementsByClassName("plus")[index];
    var text = document.getElementsByClassName("dropdown-text")[index];
    var textContent = document.getElementsByClassName("dropdown-text-content")[index];
    if(!icon.classList.contains("clicked")) {
        icon.classList.add("clicked");
        text.style.height = (textContent.clientHeight + 60) + "px";
    } else {
        icon.classList.remove("clicked");
        text.style.height = "0";
    }

}

async function getHello() {
    const response = await fetch("/data")
    const hello = await response.text();

    document.getElementById("helloTarget").innerText = hello;
}
