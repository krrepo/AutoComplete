desc 'Displays a list of available entity types for auto-complete selection'
long_desc 'takes no arguments'

command :list do |c|
  c.action do |global_options,options,args|
    ap "Executing list command ..."
    if ( !args.empty? || args.length != 0 )
      help_now! "Wrong number of arguments to 'list'"
    else
      resp = Net::HTTP.get_response(URI("http://#{$host}/typeahead/listEntities"))
      if resp.code == '200'
        puts "List of Labels:"
        entries = JSON resp.body
        entries.keys.sort.each { |k| entries[k] = entries.delete k }
        entries.each do |e|
          puts "  #{e[0]}:#{e[1]}"
        end
      else
        ap "Server Error: #{resp.code} - #{resp.message}"
      end
    end
  end
end