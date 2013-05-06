desc "Downloads an existing entity list from the server in CSV format gzip'd"
long_desc '[name] [filepath] arguments - [name] required; [filepath] optional, defaults to the list name in the current directory.'

command :download do |c|
  c.action do |global_options,options,args|
    ap "Executing download command ..."
    if (args[0] == nil)
      help_now! "Missing required argument: Name of list to download."
    else
      if (args.length > 2)
        help_now! "Too many arguments given."
      end
    end

    uristr = "http://#{$host}/typeahead/download?entity=#{args[0]}"
    resp = Net::HTTP.get_response(URI(uristr))

    if (args[1] != nil)
      filename = args[1]
    else
      filename = args[0]
    end

    begin
      file = File.open("#{filename}.csv.gz", "w")
      file.write(resp.body)
    rescue IOError => e
      ap "Failed to write file #{filename}.csv.gz."
    ensure
      file.close unless file == nil
    end

    if (resp.code == '200')
      ap "Downloaded #{args[0]} to #{filename}.csv.gz"
    else
      ap  "Server Error: #{resp.code} - #{resp.message}"
      ap "URI: #{uristr}"
      ap "response body: #{resp.body}"
    end

    ap "download command ran"
  end
end