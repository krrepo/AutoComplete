// Autocomplete
(function() {

  // Autocomplete Core
  var autocomplete = function(passedOptions) {

    function getOptions(inputControl) {
      var options = [];
      // Get Passed Options
      if (typeof passedOptions !== "undefined") {
        for (var i=0; i<passedOptions.length; i++) {
          if (typeof inputControl === 'undefined') {
            options.push(passedOptions[i]);
          } else {
            var inputControlOptions = checkForInputMatch(inputControl,passedOptions[i]);
            if (typeof inputControlOptions !== 'undefined') {
              return inputControlOptions;
            }        
          }
        }
      }
      // Get DOM Options
      var allInputs = document.getElementsByTagName("input"); // Get all input controls
      for (var i=0; i<allInputs.length; i++) {
        var input = allInputs[i];        
        if (input.type == "text") {
          var domOptions = getAutocompleteAttributes(input); // Populate the autocomplete array
          if (domOptions !== null) {
            if (typeof inputControl === 'undefined') {
              options.push(domOptions);            
            } else {
              inputControlOptions = checkForInputMatch(inputControl,domOptions);
              if (typeof inputControlOptions !== 'undefined') {
                return inputControlOptions;
              }
            }
          }
        }
      }
      return options;
    };

    //autocomplete global websocket object
    var ws;
    //autocomplete global msg item
    var msgItem;
    function setMsgInfo(target, options) {
      msgItem = {'target':target, 'options':options};
    };
    if (!ws){
      var options = getOptions();
      //see if socketIO is true for any Object
      for (var i=0; i<options.length; i++) {
        var webSocket = options[i].webSocket;
        if (webSocket) {
          var url = options[i].source.match(/[^\/\/?]?.*/)[0];
          var protocol = getProtocol(options[i].source);
          try {
            xmlhttp=new XMLHttpRequest()
            xmlhttp.onreadystatechange=function() {
              if (xmlhttp.readyState==4 && xmlhttp.status==200) {
                var hsKey=xmlhttp.responseText.match(/[^:]*/)[0];
                ws = new WebSocket(protocol+url+"websocket/"+ hsKey);
                setwsEvents();
              }
            }
            xmlhttp.open("POST", url, false);
            xmlhttp.send();
          } catch (e) { 
            safeLog('Unable to establish a websocket.'); 
          }
        break;
        }
      }
    };
    function getProtocol(url){
      var httpProtocol = url.match(/^(https?:)?/)[0];
      var wsProtocol = url.match(/^(wss?:)?/)[0];
      if ( httpProtocol.length > 1 )
        return httpProtocol;
      if ( wsProtocol.length  > 1)
        return wsProtocol;

      var curProtocol = window.location.protocol;
      if (curProtocol === 'http:')
        return 'ws:';
      else
        return 'wss:';
    };
    //set ws events
    function setwsEvents(){
      ws.onmessage = function (evt) {
        var regexp = /([^:]+):([0-9]+)?(\+)?:([^:]+)?:?([\s\S]*)?/;
        var pieces = evt.data.match(regexp);
        //check for our of interest data
        if (!pieces[5]) {
          //check for closing and prevent action
          pieces[1] === '2' ? ws.send('2:::') : {};
          return;
        }
        try {
          data = JSON.parse(pieces[5]);
        } catch (e) { 
          return;
        }
        renderDropdown(msgItem.target, data.suggestions, msgItem.options);
      };
      ws.onopen = function() {};
      ws.onclose = function() {
        safeLog('Socket has closed - refresh browser to re-establish.');
      };
    };
    function checkForInputMatch(input,options) {
      input = getObjectFromId(input);
      options.element = getObjectFromId(options.element);
      if (input === options.element) {
        return options;
      }
    };
    function init(options) {
      var options = getOptions();
      for (var i=0; i<options.length; i++) {
        setTheEvents(options[i].element,inputEvents);
      }
    };
    // ******
    // Events
    // ******
    window.onresize = function(event) {
      positionAllDropdowns();    
    }
    function focusOutHandler(event){
      if (event.relatedTarget !== null) {
        if(!event.relatedTarget.hasAttribute("glg-autocomplete-preventHide")) {
          hideDropdown(getTarget(event));
        } else {
          event.relatedTarget.addEventListener("focusout", focusOutHandler);
        }
      } else {
        hideDropdown(getTarget(event));
      }
    }

    var inputEvents = {
      'focusin':function(event) {
        showDropdown(getTarget(event));
      },
      'focusout':function(event) {
        setTimeout(function() {
          focusOutHandler(event);
        },200);
      },
      'keydown':function(event) {
        if (event.which === 9) {        // Tab key
          selectListItem(event);
          hideDropdown(getTarget(event));
        }
      },
      'keyup':function(event) {
        checkForSelectionAttributeMatch(event.target);
        if (event.which == 38) {        // Up arrow key
          moveUpList(event);
        } else if (event.which == 40) { // Down arrow key
          moveDownList(event);
        } else if (event.which == 13) { // Enter key
          selectListItem(event);
        } else if (event.which == 27) { // Escape key
          hideDropdown(getTarget(event));       
        } else if (event.which !== 9) { // As long as the tab key has not been pressed
          getDataAndRenderDropDown(getTarget(event), event);
        }
      }
    };
    var listEvents = {
      'keypress':function(event) {
        if (event.which == 38) {
          moveUpList(event);
        } else if (event.which == 40) {
          moveDownList(event);
          getTarget(event).select();      
        }
      }
    };
    var multiSelectEvents = {
      'click':function(event) {
        var target = getTarget(event);
        var multiSelectList = target.parentNode.parentNode;
        var inputKey = getKey(multiSelectList);
        var input = getInput(inputKey);
        target.parentNode.parentNode.removeChild(target.parentNode);
        positionAllDropdowns();
      }
    };

    function setTheEvents(elements, events) {
      var optionElements = [];
      var elementsType = Object.prototype.toString.call(elements);
      if (elementsType === '[object Array]' || elementsType === '[object NodeList]' || elementsType === '[object HTMLCollection]') {
        optionElements = elements;
      } else {
        optionElements.push(getObjectFromId(elements));
      }
      for (var i=0; i < optionElements.length; i++){
        optionElement = optionElements[i];
        for (var event in events) {
          if (events.hasOwnProperty(event)) {
            if ( event === "focusin" || event === "focusout" ) { //test for browser
              if ("onfocusin" in optionElement) { //IE
                optionElement.removeEventListener( event, events[event], false)
                optionElement.addEventListener( event, events[event], false)
              }
              else {
                if (event === "focusin") {
                  optionElement.removeEventListener( "focus", events[event], true)
                  optionElement.addEventListener( "focus", events[event], true)
                }
                else if (event === "focusout"){
                  optionElement.removeEventListener( "blur", events[event], true)
                  optionElement.addEventListener( "blur", events[event], true)
                }
              }
            }
            else {
              optionElement.removeEventListener( event, events[event], false)
              optionElement.addEventListener( event, events[event], false)
            }
          }
        }
      }
    };
    // **************
    // Event Handlers
    // **************
    function moveUpList(event) {
      var target = getTarget(event);
      target.select();
      var dropdown = getDropdown(target);
      var currentIndex;
      if (typeof dropdown !== 'undefined') {
        for (var i=1; i<dropdown.childNodes.length; i++) {
          if(hasClass(dropdown.childNodes[i],'glg-autocomplete-focus')) {
            currentIndex = i;
          }
        }
        removeClass(dropdown.childNodes[currentIndex],'glg-autocomplete-focus');
        currentIndex--;
        var initialItemSet = false;        
        for (var i=currentIndex; i>=0; i--) {
          if(hasClass(dropdown.childNodes[i],'glg-autocomplete-item')) {
            if (initialItemSet != true) {
              addClass(dropdown.childNodes[i],'glg-autocomplete-focus');
              initialItemSet = true;
            }
          }
        }
      }
    };
    function moveDownList(event) {
      var target = getTarget(event);    
      target.select();      
      var dropdown = getDropdown(target);
      if (typeof dropdown !== 'undefined') {
        var currentIndex;
        for (var i=0; i<dropdown.childNodes.length; i++) {
          if(hasClass(dropdown.childNodes[i],'glg-autocomplete-focus')) {
            currentIndex = i;
          }
        }
        if (typeof currentIndex === 'undefined') {
          currentIndex = 0;  
        } else if (currentIndex < dropdown.childNodes.length-1) {
          removeClass(dropdown.childNodes[currentIndex],'glg-autocomplete-focus');
          currentIndex++;      
        }
        if (currentIndex < dropdown.childNodes.length-1) {
          var initialItemSet = false;        
          for (var i=currentIndex; i<dropdown.childNodes.length; i++) {
            if(hasClass(dropdown.childNodes[i],'glg-autocomplete-item')) {
              if (initialItemSet != true) {
                addClass(dropdown.childNodes[i],'glg-autocomplete-focus');
                initialItemSet = true;
              }
            }
          }
        }
      }
    };
    function selectListItem(event) {
      var target = getTarget(event);
      var dropdown = getDropdown(target);
      var currentIndex;
      if (typeof dropdown !== 'undefined') {      
        for (var i=0; i<dropdown.childNodes.length; i++) {
          if (hasClass(dropdown.childNodes[i],'glg-autocomplete-focus')) {
            var dropdownRow = dropdown.childNodes[i].children[0];
            var key = dropdownRow.getAttribute('glg-autocomplete-key');
            var value = dropdownRow.getAttribute('glg-autocomplete-value');
            var display =  dropdownRow.getAttribute('glg-autocomplete-display');
            var preventHide = dropdownRow.getAttribute('glg-autocomplete-preventHide');
            setSelectionValue(target,key,value,display,'keypress',dropdownRow,preventHide);
          }
        }
      }
    };
    function setSelectionValue(input,key,value,display,type,dropdownRow,preventHide) {
      var options = getOptions(input);
      multiSelect = options.multiSelect
      if (multiSelect) {
        // Add the Clicked Item to the MultiSelect
        var multiSelectItems = getMultiSelectItems(input);
        if (typeof multiSelectItems === 'undefined') {
          createMultiSelectItems(input,display);
        } else {
          addToMultiSelectItems(input,multiSelectItems,display)
        }
      } else {
        // Add the Clicked Item Directly to the Input
        input.value = display;
        input.setAttribute("glg-autocomplete-key",key);
        input.setAttribute("glg-autocomplete-value",value);
        input.setAttribute("glg-autocomplete-display",display);
      }
 
      // Fire Custom Event - Does not support IE6
      if ("dispatchEvent" in input) {
        var selectEvent = new CustomEvent(
          "glgAutocompleteSelect",
          {
            detail: {
              key: key,
              value: value,
              display: display,
              type: type,
              dropdown: dropdownRow
            },
            bubbles: true,
            cancelable: true
          }
        );
        input.dispatchEvent(selectEvent);
      }
 
      // Fire Change Event
      if ("fireEvent" in input) {
        input.fireEvent("onchange");
      } else {
        var changeEvent = document.createEvent("HTMLEvents");
        changeEvent.initEvent("change", false, true);
        input.dispatchEvent(changeEvent);
      }
 
      // Hide Dropdown
      if (typeof preventHide === 'undefined') {
        hideDropdown(input);
      }
 
    }
    function updateServiceTerms(targetInput,event) {
      var target = getTarget(event);
      var value = event.target.innerHTML;
      var attributes = getOptions(targetInput)
      var options = getOptions(target);
      if (typeof attributes.updateSource !== 'undefined') {
        var counter = 0;
        var currentEntity;
        for (entity in attributes.entities) {
          counter++;
          currentEntity = entity
        }
        if (counter == 1) {
          callbackST = function ( data ) {
           return;
         }
          var url = attributes.updateSource;
          if (!url.search(socketio)){
            url+="?entity="+currentEntity+"&value="+value;
            var serviceTermsElement = document.createElement('script');
            serviceTermsElement.type = "text/javascript";
            serviceTermsElement.src = url+"&callback=callbackST";
            document.getElementsByTagName('head')[0].appendChild(serviceTermsElement);
          } else{
            sendSocketMessage(options, currentEntity, value);
          }
        }
      }
    };
    // ******
    // Render
    // ******
    function checkForSelectionAttributeMatch(input) {
      var displayAttribute = input.getAttribute('glg-autocomplete-display');
      var inputText =  input.value;
      if (displayAttribute !== null && inputText !== "") {
        if (displayAttribute !== inputText) {
          input.removeAttribute('glg-autocomplete-key');
          input.removeAttribute('glg-autocomplete-value');
          input.removeAttribute('glg-autocomplete-display');
        }
      }
    }
    function getDataAndRenderDropDown(target, event) {
      var options = getOptions(getTarget(event));
      var url = options.source + "?value="+encodeURIComponent(target.value);
      var entity = "";
      for (optionItem in options.entities) {
        if (options.entities.hasOwnProperty(optionItem)) {
          entity = encodeURIComponent(optionItem)
          url+="&entity="+entity;
        }
      }

      if (target.value !== "") {
        sendSocketMessage = function () {
          var msg=target.value;
          var jsonObject = {'@class': 'com.glg.service.TrieObject',
            entity:entity,
            prefix:msg};
          ws.send('4:::'+JSON.stringify(jsonObject));
        }
        
        if (options.webSocket){
          setMsgInfo(target, options);
          sendSocketMessage();
        } else {

          var timeIndex = new Date().getTime().toString();
 
          callbackRD = function (data) {
            renderDropdown(target, data, options);
            var scriptTag = document.getElementById(timeIndex);
            if (scriptTag !== null) {
              scriptTag.parentNode.removeChild(scriptTag);
            }
          }

          var dropdownElement = document.createElement('script');
          dropdownElement.type = "text/javascript";
          dropdownElement.src = url+"&callback=callbackRD&"+timeIndex;
          dropdownElement.id = timeIndex
          document.getElementsByTagName('head')[0].appendChild(dropdownElement);
        }
      } else {
        removeDropdown(target);
      }
    };
    
    function renderDropdown(target, data, options) {
      removeDropdown(target);
      var dropdown = "";
      if (typeof options.specialFirstRow !== 'undefined') {
        dropdown += '<li glg-autocomplete-firstRow class="glg-autocomplete-item glg-autocomplete-focus">'+options.specialFirstRow()+'</li>'
      }
      var entityCount = 0;
      var drawAddToItem = true;
      for (entityData in data) {
        if (data.hasOwnProperty(entityData)) {
          entityCount++;
        }
      }
      var entityDataHit = false;
      for (entityData in data) {
        if (data.hasOwnProperty(entityData)) {
          var entities = data[entityData];
          if (typeof entities !== 'undefined') {
            if (entities.length > 0) {
              if (entityCount > 1 || options.showCategoryNames) {
                dropdown += '  <li class="glg-autocomplete-category">'+options.entities[entityData]+'</li>';
              }
              // Generate the dropdown code
              for (var i=0; i<entities.length; i++) {
                dropdown += '  <li class="glg-autocomplete-item" role="presentation">';
                entityDataHit = true;

                display = entities[i].display;
                key = entities[i].key;
                value = entities[i].value;

                // If display exists, us it as is, otherwise use value, stripped of quotation marks.
                if ((display == null) || (display.length == 0)) {
                  display = value
                }
                if (typeof options.rowRenderer === 'function') {
                  dropdown += options.rowRenderer(i, key, value, display);
                } else {
                  dropdown += '    <a id="ui-id-' + i + '" class="glg-autocomplete-anchor" glg-autocomplete-key="'+ key +'" glg-autocomplete-value="'+ value +'" glg-autocomplete-display="'+ display +'" tabindex="-1">' + display + '</a>';
 
                }
 
                dropdown += '  </li>';
                if (entities[i].value.toLowerCase() == target.value.toLowerCase()) {
                  drawAddToItem = false;
                }
              }
            }
          }
        }
      }
 
      // Clear the Dropdown Code if No Entities Were Populated
      if (!entityDataHit) {
        dropdown = '';
      }
 
      // Create the 'Add to' Menu
      if (options.updateSource && drawAddToItem) {
        dropdown += '  <li class="glg-autocomplete-category">Click to Add</li>';
        dropdown += '  <li class="glg-autocomplete-item" role="presentation">';
        dropdown += '    <a id="ui-id-' + i + '" class="glg-autocomplete-anchor" tabindex="-1">' + target.value + '</a>';
        dropdown += '  </li>';
      }
 
      if(dropdown.length > 0) {
        // Decorate the Dropdown Node
        var dropdownNode = document.createElement("ul");
        dropdownNode.setAttribute('class','glg-autocomplete-list');
        dropdownNode.innerHTML = dropdown;      
        positionDropdown(dropdownNode,target,true);

        // Set the Unique Identifiers if They Do Not Already Exist
        var inputKey = target.getAttribute('data-glg-dropdown-input');
        if (inputKey === null) {
          inputKey = new Date().toJSON();
          target.setAttribute('data-glg-dropdown-input',inputKey);
        }
        dropdownNode.setAttribute('data-glg-dropdown-input',inputKey);
 
        // Insert the Dropdown Node
        target.parentNode.insertBefore(dropdownNode, target.nextSibling);

        // Set Click Handlers
        listElements = document.getElementsByClassName("glg-autocomplete-anchor");
        for (var i=0; i<listElements.length; i++) {
          listElements[i].onclick = listItemClick;
        }
      }
 
      // Fire Custom Event - Does not support IE6
      if ("dispatchEvent" in target) {
        var selectEvent = new CustomEvent(
          "glgAutocompleteRendered",
          {
            detail: {
              data: data,
              dropdownNode: dropdownNode
            },
            bubbles: true,
            cancelable: true
          }
        );
        target.dispatchEvent(selectEvent);
      }
    }
 
    function getAncestorAttribute(element,attribute) {
      if (element.hasAttribute(attribute)) {
        return element.getAttribute(attribute);
      } else if (element.parentElement !== null) {
        return getAncestorAttribute(element.parentElement,attribute);
      }
    }
 
    function listItemClick(event) {
      var target = getTarget(event);
      var key =  getAncestorAttribute(event.target,'glg-autocomplete-key');
      var value =  getAncestorAttribute(event.target,'glg-autocomplete-value');
      var display = getAncestorAttribute(event.target,'glg-autocomplete-display');
      var dropdownKey = getAncestorAttribute(event.target,'data-glg-dropdown-input');
      var preventHide =  getAncestorAttribute(event.target,'glg-autocomplete-preventHide');
      var allInputs = document.getElementsByTagName("input"); // Get all input controls
      var multiSelect = false;
      var targetInput = "";
      for (var i=0; i<allInputs.length; i++) {
        var input = allInputs[i];
        if (input.hasAttribute("data-glg-dropdown-input")) {
          var inputKey = input.getAttribute("data-glg-dropdown-input");
          if (inputKey == dropdownKey) {
            targetInput = input;
            setSelectionValue(targetInput,key,value,display,'click',target,preventHide)
          }
        }
      }
      updateServiceTerms(targetInput,event);
    }
 
    function convertCssPixelsToNumbers(cssPixel) {
      if (cssPixel === "" || cssPixel === null || typeof cssPixel === "undefined") {
        return 0;
      } else {
        return parseInt(cssPixel.split("p")[0]);
      }
    }
    function getWidthWithMargins(element) {
      var width = 0;
      var offsets = ['margin-left', 'margin-right', 'padding-right', 'padding-left']
      for (var i=0; i<offsets.length; i++) {
        width += parseInt(getComputedStyle(element, offsets[i]).getPropertyValue(offsets[i]).split("p")[0]);
      }
      return element.offsetWidth + width;
    }
    function updateMultiSelectSpacing(input, multiSelectNode) {
      // Configuration
      var inputLeftPadding = parseInt(getComputedStyle(input, 'padding-left').getPropertyValue('padding-left').split("p")[0]);
      var inputRightPadding = parseInt(getComputedStyle(input, 'padding-right').getPropertyValue('padding-right').split("p")[0]);
      var inputBorderLeftWidth = parseInt(getComputedStyle(input, 'border-left-width').getPropertyValue('border-left-width').split("p")[0]) || 3;
      var inputBorderRightWidth = parseInt(getComputedStyle(input, 'border-left-width').getPropertyValue('border-left-width').split("p")[0]) || 3;
      var inputTopPadding = parseInt(getComputedStyle(input, 'padding-top').getPropertyValue('padding-top').split("p")[0]);
      var inputBottomPadding = parseInt(getComputedStyle(input, 'padding-bottom').getPropertyValue('padding-bottom').split("p")[0]);
      var inputLineHeight = parseInt(getComputedStyle(input, 'line-height').getPropertyValue('line-height').split("p")[0]);
 
      // Position the MultiSelectNode
      multiSelectNode.style.left = input.offsetLeft + "px";
      multiSelectNode.style.top = input.offsetTop + "px";

      // Set the Height
      if (input.offsetHeight > inputLineHeight*2) {
        if (multiSelectNode.offsetHeight == 0) {
          input.style.paddingTop = '1px';
        } else {
          input.style.paddingTop = multiSelectNode.offsetHeight + 'px';
        }
      } else {
        // Set the Padding for the Input
        var multiSelectWidth = getWidthWithMargins(multiSelectNode);
        if (multiSelectWidth > 0 && multiSelectNode.offsetHeight <= input.offsetHeight) {
          input.style.width = input.offsetWidth-multiSelectWidth-inputRightPadding-inputBorderLeftWidth-inputBorderRightWidth + "px";
          input.style.paddingLeft = multiSelectWidth + 'px';
        } else if (multiSelectWidth > 0 && multiSelectNode.offsetHeight > input.offsetHeight) {
          input.style.paddingTop = multiSelectNode.offsetHeight + 'px';
          input.style.width = input.offsetWidth-1-inputRightPadding-inputBorderLeftWidth-inputBorderRightWidth + 'px';
          input.style.paddingLeft = '1px';
        } else {
          input.style.width = input.offsetWidth-1-inputRightPadding-inputBorderLeftWidth-inputBorderRightWidth + 'px';
          input.style.paddingLeft = '1px';
          input.style.height = input.style.lineHeight + 'px';
        }
      }
    }
    function getKey(element) {
      var key1 = element.getAttribute('data-glg-dropdown-input');
      var key2 = element.getAttribute('data-glg-dropdown-inputSelections');
      return key1 || key2;
    }
    function getInput(inputKey) {
      return getElementByKey('input',inputKey);
    }
    function getElementByKey(elementType,searchKey) {
      var matchingElement;      
      elements = document.getElementsByTagName(elementType);
      for (var i=0; i<elements.length; i++) {
        elementKey = getKey(elements[i]);
        if (elementKey == searchKey) {
          matchingElement = elements[i];
        }
      }
      return matchingElement;
    }
    function positionDropdown(dropdownNode, target, show) {
      // Position and Insert the Dropdown Node
      var options = getOptions(target);
      if (options.width === null) {
      } else if (typeof options.width === 'number') {
        dropdownNode.setAttribute('style','display: block; min-width: '+options.width+';');
      } else {
        dropdownNode.setAttribute('style','display: block; width: '+target.offsetWidth+';');
      }
      dropdownNode.style.left = target.getBoundingClientRect().left + document.body.scrollLeft + "px";
      dropdownNode.style.top = target.getBoundingClientRect().top + target.offsetHeight + document.body.scrollTop + "px";
      if (!show) {
        dropdownNode.style.display = 'none'
      }
    }
    function positionAllDropdowns() {
      // Get Inputs
      var inputs = {};
      var allInputs = document.getElementsByTagName("input"); // Get all input controls
      for (var i=0; i<allInputs.length; i++) {
        var input = allInputs[i];        
        if (input.hasAttribute("data-glg-dropdown-input")) {
          inputs[input.getAttribute("data-glg-dropdown-input")] = input;
        }
      }
      // Set UL Positions
      //Step 1 - for each multi-select node
      elements = document.getElementsByClassName('glg-autocomplete-list');
      for (var i=0; i<elements.length; i++){
        var node = null;
        var dropdownKey = null;
        for (var j=0; j<elements[i].parentNode.childNodes.length; j++) {
          if ( hasClass(elements[i].parentNode.childNodes[j],"glg-autocomplete-multiSelect") ) {
            node = elements[i].parentNode.childNodes[j];
            dropdownKey  = node.getAttribute('data-glg-dropdown-inputselections');;
          }
        }
        //Step 2 - get all list controls and position those
        if ( node != null && dropdownKey != null ) { //no dropdown so no need to go further 
          var allLists = document.getElementsByTagName("ul"); // Get all the lists
          for (var k=0; k<allLists.length; k++) {
            var list = allLists[k];
            if (list.hasAttribute("data-glg-dropdown-input")) {
              positionDropdown(list,inputs[list.getAttribute("data-glg-dropdown-input")], false);
              //Step 3 -  re-space mutiSelect items
              var inputKey = list.getAttribute("data-glg-dropdown-input");
              var multiSelectItem = inputs[list.getAttribute("data-glg-dropdown-input")];
              if (inputKey === dropdownKey) {
                var options = getOptions(multiSelectItem);
                if ( multiSelectItem.id === options.element.id ) {
                  if (typeof options.multiSelect != 'undefined' && options.multiSelect ) {
                    updateMultiSelectSpacing(multiSelectItem, node);
                  }
                }
              }
            }
          }
        }
      }
    }
    function removeDropdown(target) {
      var siblings = getSiblings(target)
      for (var i=0; i<siblings.length; i++) {
        if (typeof siblings[i].getAttribute === 'function') {
          if (siblings[i].getAttribute('class') === 'glg-autocomplete-list') {
            siblings[i].parentNode.removeChild(siblings[i]);
          }
        }
      }
    }
    function hideDropdown(target) {
      var dropdown = getDropdown(target)
      if (typeof(dropdown) !== 'undefined') {      
        dropdown.style.display = 'none';
      }
      // Fire Custom Event - Does not support IE6
      if ("dispatchEvent" in target) {
        var selectEvent = new CustomEvent(
          "glgAutocompleteHidden",
          {
            detail: {
              dropdownNode: dropdown
            },
            bubbles: true,
            cancelable: true
          }
        );
        target.dispatchEvent(selectEvent);
      }
    }
    function showDropdown(target) {
      var key = getKey(target);
      var input = getInput(key);
      if (input.value !== "") {
        var dropdown = getDropdown(target)
        if (typeof(dropdown) !== 'undefined') {
          dropdown.style.display = 'block';
        }
      }
    }
    // MultiSelect
    function addToMultiSelectItems(input,multiSelectItems,value) {
      // Construct the MultiSelect Child Node
      var multiSelectElement = '<div class="glg-autocomplete-multiSelectElementClose">X</div><span class="value">'+value+'</span>';
      var multiSelectElementNode = document.createElement('div');
      multiSelectElementNode.innerHTML = multiSelectElement;
      multiSelectElementNode.setAttribute('class','glg-autocomplete-multiSelectElement');
 
      // Insert the Child Node, Adjust Spacing, and Set Events
      multiSelectItems.appendChild(multiSelectElementNode);                           // Insert the MultiSelect Child Node
      positionAllDropdowns();                                                         // Adjust Positioning
      elements = document.getElementsByClassName('glg-autocomplete-multiSelectElementClose')
      setTheEvents(elements,multiSelectEvents);                                       // Set Events
      input.value = '';                                                               // Clear the Inputs
      return multiSelectNode;
    }
    function createMultiSelectItems(input,value) {
      // Construct the MultiSelect Node
      multiSelect = '<div class="glg-autocomplete-multiSelectElement"><div class="glg-autocomplete-multiSelectElementClose">X</div><span class="value">'+value+'</span></div>';
      multiSelectNode = document.createElement('div');
      var inputKey = input.getAttribute('data-glg-dropdown-input');
      multiSelectNode.innerHTML = multiSelect;
      multiSelectNode.setAttribute('data-glg-dropdown-inputSelections',inputKey);
      multiSelectNode.setAttribute('class','glg-autocomplete-multiSelect');
      multiSelectNode.style.maxWidth = input.offsetWidth + 'px';
 
      // Insert the MultiSelect Node, Adjust Positioning, and Set Events
      input.parentNode.insertBefore(multiSelectNode, input.nextSibling);              // Insert the MultiSelect Node
      positionAllDropdowns();                                                         // Adjust Positioning
      elements = document.getElementsByClassName('glg-autocomplete-multiSelectElementClose')
      setTheEvents(elements,multiSelectEvents);                                       // Set Events
      input.value = '';                                                               // Clear the Inputs 
      return multiSelectNode;
    }
 
    // *********
    // Utilities
    // *********
    function getAutocompleteAttributes(input) {
      var autocompleteAttributeMap = {'entities':'glg-autocomplete-entities','source':'glg-autocomplete-source','updateSource':'glg-autocomplete-updateSource','multiSelect':'glg-autocomplete-multiselect','webSocket':'glg-autocomplete-websocket'};
      var autocompleteAttributes;
      for(var key in autocompleteAttributeMap) {
        if (autocompleteAttributeMap.hasOwnProperty(key)) {
          var attribute = input.getAttribute(autocompleteAttributeMap[key]);
          if (attribute !== null) {
            if (typeof autocompleteAttributes === 'undefined') {
              autocompleteAttributes = {};
            }
            if ( key === "multiSelect" || key === "webSocket") {
              var myBoolean = attribute === "true";
              autocompleteAttributes[key] = myBoolean;
            } else {
              autocompleteAttributes[key] = attribute;
            }
          }  
        }
      }

      // Parse the Entity
      if (typeof autocompleteAttributes !== 'undefined') {
        var tempAttributes = autocompleteAttributes.entities.split(";");
        autocompleteAttributes.entities = {};        
        for (var i=0; i<tempAttributes.length; i++) {
          var tempAttribute = tempAttributes[i].split(":")
          autocompleteAttributes.entities[tempAttribute[0]] = tempAttribute[1] 
        }
      }

      if (typeof autocompleteAttributes === 'undefined') {
        return null;
      } else {
        autocompleteAttributes.element = input;
        return autocompleteAttributes;
      }
    };
    function getTarget(evt){
     evt = evt || window.event; // get window.event if argument is falsy (in IE)
     // get srcElement if target is falsy (IE)
     var targetElement = evt.target || evt.srcElement;
     //return id of <li> element when hovering over <li> or <a>
     if (targetElement.nodeName.toLowerCase() === 'li') {
       return targetElement;
     } else if (targetElement.parentNode.nodeName.toLowerCase() === 'li' && targetElement.nodeName.toLowerCase() !== 'input') {
       return targetElement.parentNode;
     } else {
       return targetElement;
     }
    }
    function getObjectFromId(id) {
      if (typeof id === 'string') {
        return document.getElementById(id);
      } else {
        return id;
      }
    }
    function getDropdown(target) {
      var parentClasses = target.parentElement.classList
      for (var i=0; i<parentClasses.length; i++) {
        if (parentClasses[i] === 'glg-autocomplete-list') {
          return target.parentElement
        }
      }
      var siblings = getSiblings(target)
      for (var i=0; i<siblings.length; i++) {
        if (typeof siblings[i].getAttribute === 'function') {
          if (siblings[i].getAttribute('class') === 'glg-autocomplete-list') {
            return siblings[i];
          }
        }
      }
    }
    function getSiblings(element) {
      var elem = element;
      var siblings = [];
      while (elem.nextSibling !== null) {
        siblings.push(elem.nextSibling);
        elem = elem.nextSibling;
      }
      var elem = element;
      while (elem.previousSibling !== null) {
        siblings.push(elem.previousSibling);
        elem = elem.previousSibling;
      }
      return siblings;
    }
    function getMultiSelectItems(target) {
      var multiSelectList;
      var allLists = document.getElementsByTagName('div'); // Get all list controls
      for (var i=0; i<allLists.length; i++) {
        var list = allLists[i];
        var listKey = list.getAttribute('data-glg-dropdown-inputSelections');
        var targetKey = target.getAttribute('data-glg-dropdown-input');
        if (listKey == targetKey) {
          multiSelectList = list;
        }
      }
      return multiSelectList;
    }
    function hasClass(element,className) {
      if (typeof element.classList !== 'undefined') {
        var classes = element.classList;
        for (var j=0; j<classes.length; j++) {
          if (classes[j] == className ) {
            return true;
          }
        }
      } else {
        if ( typeof element.className && element.className === className){
          return true;
        }
      }
      return false;
    };
    function removeClass(element,className) {
      if (typeof element.classList !== 'undefined') {
        var classes = element.getAttribute('class').split(" ");
        for (var i=0; i<classes.length; i++) {
          if (classes[i] == 'glg-autocomplete-focus') {
            classes.splice(i,1);
            element.setAttribute('class',classes.join(" "));
            return true
          }
        }
      }
      return false;
    };
    function addClass(element,className) {
      var classes = element.getAttribute('class').split(" ");
      classes.push('glg-autocomplete-focus')            
      element.setAttribute('class',classes.join(" "));
      return true;
    };
    
    function safeLog(msg){
      if (typeof console.log != undefined) {
        console.log(msg);
      }
    };

    if (typeof CustomEvent === 'object') {
      function CustomEvent ( event, params ) {
        params = params || { bubbles: false, cancelable: false, detail: undefined };
        var evt = document.createEvent( 'CustomEvent' );
        evt.initCustomEvent( event, params.bubbles, params.cancelable, params.detail );
        return evt;
      };
    };

  // Initialization Functions
  if (document.readyState === "complete") {
    init();
    } else {
    window.onload = function() {
      init();
    };
  }
 }
 
  // Library Detection
  if (typeof define === "function") {     // RequireJS
    define(function() {
      return autocomplete;
    });
  } else {                                // Global Object (no Library)
    if (typeof(glg) === 'undefined') {
      glg = {}
    }
    glg['autocomplete'] = autocomplete;
    autocomplete();
  }
 
  if (typeof angular === "object") {      // Angular
    angular.module('Autocomplete', [])
    .factory('Autocomplete', function() {
      return autocomplete;
    })
    .run(function($rootScope) {
      $rootScope.$on('$viewContentLoaded', function () {
        autocomplete();
      })
    })
  }
 
})();