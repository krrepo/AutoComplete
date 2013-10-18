desc 'Updates an existing value on an entity list. If the value does not exist, it will be added.'
long_desc '[name] [key] [value] [rank] [display] are all required; e.g., tasc update state massachusetts MA 1 Massachusetts'

command :update do |c|
  c.action do |global_options,options,args|

    ap "Executing update command ..."
    if (args[0] == nil) || (args[1] == nil)  || (args[2] == nil) || (args[3] == nil) || (args[4] == nil)
      help_now! "Missing required arguments to update command."
    else
      if (args.length > 5)
        help_now! "Too many arguments given."
      else
        uristr = "http://#{$host}/typeahead/update?entity=#{args[0]}&key=#{args[1]}&value=#{args[2]}&rank=#{args[3]}&display=#{args[4]}"
        uri = URI(uristr)
        resp = Net::HTTP.get_response(uri)
        if (resp.code == '200')
          ap "Added #{args[1]} to #{args[0]} with value #{args[2]} rank #{args[3]} display #{args[4]}"
        else
          ap  "Server Error: #{resp.code} - #{resp.message}"
          ap "URI: #{uristr}"
          ap "response body: #{resp.body}"
        end
      end
    end

  end
end