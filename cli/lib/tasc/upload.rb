desc "Upload a CSV file (gzip'd or not) containing a comma-separated list of values and meta-data on which to auto-complete.  Replaces an existing entity list if it already exists, creates a new one otherwise."
long_desc '[filepath] argument required'

command :upload do |c|
  c.action do |global_options,options,args|
    unless args.empty?
      filepath = args[0]
      url = "http://#{$host}/typeahead/upload"
      # see http://stackoverflow.com/questions/184178/ruby-how-to-post-a-file-via-http-as-multipart-form-data
      # and https://wincent.com/products/wopen3
      # orig: curl -F media=@#{photo.path} -F username=#{@username} -F password=#{@password} -F message='#{photo.title}' http://twitpic.com/api/uploadAndPost`
      # adapted: curl -F file=@#{filepath} #{url}
      file = "file=@#{filepath}"
      result = Wopen3.system('curl', '-F', file, url)
      if result.success?
        puts "Successfully uploaded '#{filepath}'"
        puts "stdout: #{result.stdout}"
        puts "stderr: #{result.stderr}"
      else
        puts "Failed to upload"
        puts "stdout: #{result.stdout}"
        puts "stderr: #{result.stderr}"
      end
    else
      help_now! "Wrong number of arguments to 'upload'"
    end
  end
end
