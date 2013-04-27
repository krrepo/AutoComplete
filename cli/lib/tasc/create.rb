desc 'Create a self-maintaining entity list'
long_desc '[name] [maxSize] arguments - [name] is required; [maxSize] is optional and will default to 10,000'

command :create do |c|
  c.action do |global_options,options,args|
    if (args[0] == nil)
      help_now! "Missing required argument to create command: Name of list to create."
    else
      if (args[1] == nil)
        args[1] = 10000
      end
      if (args[1].to_i < 1)
        args[1] = 10000
      end
      # Construct URL to REST-ful service
      uristr = "http://#{$host}/typeahead/createCache?entity=#{args[0]}&maxSize=#{args[1]}"
      uri = URI(uristr)
      resp = Net::HTTP.get_response(uri)
      if (resp.code == '200')
        ap "List created: #{args[0]}"
      else
        ap  "Server Error: #{resp.code} - #{resp.message}"
        ap "URI: #{uristr}"
        ap "response body: #{resp.body}"
      end
    end
  end
end