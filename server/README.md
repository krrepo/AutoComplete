# TrieService

## Installation

Installation of the service presumes that java, git, and buildr are installed on the target host.

1. Clone this repository to your target host `git clone https://github.com/glg/AutoComplete.git`
1. Two manual configuration adjustments are needed depending on your local environment:
   * edit server/trieservice.conf and set the start-up folder on line 11
   * edit server/install.sh and set JAVA_HOME
1. After these adjustments have been made, change your working directory to the newly created repo's server folder and run `sudo chmod go+rx install.sh`
1. Then build and deploy the autosuggest service using `sudo ./install.sh`
   * see `http://<targethost>/example.html` to verify service installation

See the [tasc cli](AutoSuggest/tree/master/cli), and the API section below for the list of exposed endpoints

## Server API
* [trie](#typeaheadtrie)
* [listTrie](#typeaheadlisttrie)
* [listEntities](#typeaheadlistentities)
* [createCache](#typeaheadcreatecache)
* [update](#typeaheadupdate)
* [selected](#typeaheadselected)
* [deleteTrie](#typeaheaddeletetrie)
* [remove](#typeaheadremove)
* [setNumResults](#typeaheadsetnumresults)
* [upload](#typeaheadupload)
* [download](#typeaheaddownload)


####typeahead/trie
Return a list of suggestions filtered by entity, typeahead value, and configured number of results.  Requests will be returned as either JSON, or as JSONP if the option parameter 'callback' is appended to the request with a value of the desired return function.  Accepts the following parameters:
  * callback (Optional)
  * entity - (Multiple Instances Allowed)
  * value (Search Parameter)

Example Call
```
http://localhost:8080/typeahead/trie?value=d&entity=state&entity=org&callback=callbackRD
```
Example Response
```
callbackRD({"state":["DE","DC"]})
```

####typeahead/listTrie
Return an existing entity list.  Accepts the following parameters:
  * entity

Example Call
```
http://localhost:8080/typeahead/listTrie?entity=state
```
Example Response
```
[["alabama","AL","1"],["alaska","AK","1"],["american samoa","AS","1"]]
```

####typeahead/listEntities
Return a list of available entity types for auto-complete selection.

Example Call
```
http://localhost:8080/typeahead/listEntities
```
Example Response
```
["state","org"]
```

####typeahead/createCache
Create a self-maintaining entity list.  Accepts the following parameters:
  * entity
  * maxSize (Numeric)

Example Call
```
http://localhost:8080/typeahead/createCache?entity=state&maxSize=60
```

####typeahead/update
Update an existing value on an entity list. If the value does not exist, it will be added.  Accepts the following parameters:
  * entity
  * key
  * value
  * rank

Example Call
```
http://localhost:8080/typeahead/update?entity=state&key=myKey&value=myValue&rank=myRank
```

####typeahead/selected
Increment rank of an existing value on an entity.  Adds the value to a mutable entity. Accepts the following parameters:
params:
  * entity
  * value

Example Call
```
http://localhost:8080/typeahead/selected?entity=state&value=myValue
```

####typeahead/deleteTrie
Delete an entity list from the server.  Accepts the following parameters:
  * entity

Example Call
```
http://localhost:8080/typeahead/deleteTrie?entity=state
```

####typeahead/remove
Remove an existing key and its associated value from an entity list.  Accepts the following parameters:
  * entity
  * key

Example Call
```
http://localhost:8080/typeahead/remove?entity=state&key=myKey
```

####typeahead/setNumResults
Configure the response size max.  Accepts the following parameters:
  * results (Numeric)

Example Call
```
http://localhost:8080/typeahead/setNumResults?results=10
```

####typeahead/download
Download an existing entity list from the server in CSV format gzip'd.  Accepts the following parameters:
  * entity

Example Call
```
http://localhost:8080/typeahead/download?entity=state
```

####typeahead/upload
Upload a CSV file (gzip'd or not) containing a comma-separated list of values and meta-data on which to auto-complete.  Accepts the following parameter:

params:
  * filepath

Example Call
```
curl -F "file=@states.csv" http://localhost:8080/typeahead/upload
```

Format
```
column 1 : Value
column 2 : Key
column 3 : Rank (Numeric, Optional)
```
