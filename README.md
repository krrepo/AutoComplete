# AutoComplete
A total, "soup-to-nuts" auto-complete / auto-suggest / type-ahead solution

A Java/Jersey/Grizzly RESTful service backed by a trie
data structure, a cross-browser and dependency-free JavaScript plug-in that
extends the HTML input element by rendering a list of suggested options
in response to user input, and a command-line client for managing
keyword lists used by the service

### Features
* Selecting an item will replace the text in the input element with the selection value
* Enables separation of display text from input value
  (e.g., match on 'massachu', but submit 'MA')
* Optional multi-select support allows list item selections to function as appends rather than replacements
* Manage multiple lists of your own choosing
  (e.g., list of names, states, countries, companies, products,...)
* Supports both static and dynamic / self-maintaining lists
  useful for tagging applications
* Optionally suggest matches from across several lists at once
* Handles diacriticals
 
### Support
* JavaScript plug-in tested with Chrome, Firefox, Safari, and IE9
* Server tested with Ubuntu 11.10
* CLI tested under Ruby 1.9.3 on Ubuntu, and Mac OS X

### Installation and Usage
1. Build and deploy the
   **[service](./server)**
1. Install the **[command line client](./cli)**
1. Utilize the **[JavaScript plug-in](./web)**
1. Enjoy!

### Thanks to the Contributors
[apcollier](../../../../apcollier-glg), [asegal](../../../../asegal),
[bchee](../../../../bwkchee), [dgriffis](../../../../dgriffis),
and [squince](../../../../squince)
