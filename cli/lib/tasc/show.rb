desc 'Display an existing entity list'
long_desc '[name] argument required'

command :show do |c|
  c.action do |global_options,options,args|
    if ( args.empty? || args.length != 1 )
      help_now! "Wrong number of arguments to 'show'"
    else
      name = args[0]
      resp = Net::HTTP.get_response(URI("http://#{$host}/typeahead/listTrie?entity=#{name}"))
      if resp.code == '200'
        entries = JSON resp.body
        puts "Entries from #{name}:"
        puts Hirb::Helpers::Table.render entries, :headers=>["Key", "Value",  "Weight", "Display"]
      elsif resp.code == '202'
        puts "List '#{name}' too long to display"
      elsif resp.code == '204'
        puts "Unknown list: '#{name}'"
      else
        puts "Server Error: #{resp.code} - #{resp.message}"
      end
    end
  end
end