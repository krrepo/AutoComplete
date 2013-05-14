# autocomplete.js
### Usage
Can be used with either JavaScript method calls, markup declarations on
input elements, or a combination of the two.

To include in your webpage, just include the js/autocomplete.js and css/autocomplete.css as source tags:
  ```js
    <script src="js/autocomplete.js"></script>
    <link type="text/css" rel="stylesheet" href="css/autocomplete.css"/>
  ```

Then choose an implementation style and replace _server:port_ to point
at the AutoComplete service of your choice:

  * Method Calls: Pass in a configuration object to the glglabs.autocomplete method.
    ```js
    <script>
     options = [{
       'entities':{
         'org':'Organizations',
         'state':'States'
       },
       'source':'//server:port/typeahead/trie/',
       'element':document.getElementById('tags'),
       'updateSource'='//server:port/typeahead/update/'
     }];
     
     glg.autocomplete(options);
    </script>
    ```

  * Markup Declarations: Set attributes on any input element you wish to use with autocomplete.
    ```js
    <input
      glg-autocomplete-entities='state:States'
      glg-autocomplete-source='//server:port/typeahead/trie/'
      glg-autocomplete-updateSource='//server:port/typeahead/update/'
    >
    ```

For usage example, see `example.html`
