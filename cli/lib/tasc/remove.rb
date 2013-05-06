desc 'Removes an existing key and its associated value from an entity list'
long_desc '[name] [key] arguments are required'

command :remove do |c|
  c.action do |global_options,options,args|

    ap "Executing remove command ..."
    if (args[0] == nil) || (args[1] == nil)
      help_now! "Missing required arguments to remove command."
    else
      if (args.length > 2)
        help_now! "Too many arguments given."
      else
        uristr = "http://#{$host}/typeahead/remove?entity=#{args[0]}&key=#{args[1]}"
        uri = URI(uristr)
        resp = Net::HTTP.get_response(uri)
        if (resp.code == '200')
          ap "Removed #{args[1]} from #{args[0]}"
        else
          ap  "Server Error: #{resp.code} - #{resp.message}"
          ap "URI: #{uristr}"
          ap "response body: #{resp.body}"
        end
      end
    end

  end
end