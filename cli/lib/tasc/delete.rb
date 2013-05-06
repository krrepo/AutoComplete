desc 'Deletes an entity list from the server'
long_desc '[name] argument required'

command :delete do |c|
  c.action do |global_options,options,args|
    ap "Executing delete command"
    if ( args[0] == nil)
      help_now! "Missing required argument: Name of list to remove."
    else
      if ( args.length > 1 )
        help_now! "Delete takes only one argument."
      else
        uristr = "http://#{$host}/typeahead/deleteTrie?entity=#{args[0]}"
        resp = Net::HTTP.get_response(URI(uristr))
      end
    end
    if (resp.code == '200')
      ap "Delete command successful. #{args[0]} deleted."
    else
      ap "Server Error: #{resp.code} - #{resp.message}"
    end
  end
end