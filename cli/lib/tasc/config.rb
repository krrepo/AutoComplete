desc 'Lets you add, remove, list, or use a service connection'
long_desc 'Controls the configuration of tasc. The default sub-command is "list"'

command :config do |c|

  c.desc 'Displays the configured service connections'
  c.long_desc 'Arguments are ignored'
  c.command :list do |l|
    l.action do |global_options,options,args|
      unless args.empty?
        ap "Warning: Arguments to 'config list' are ignored."
      end
      entries = $tasc_conf.to_a
      puts "Configuration:"
      puts Hirb::Helpers::Table.render entries, :description=>false, :headers=>["Key", "Value"]
    end
  end

  c.desc 'Switch tasc to use a different service'
  c.long_desc '[label] argument required.  Changes the value of "current_server". e.g., tasc use dev'
  c.command :use do |u|
    u.action do |global_options,options,args|
      if (args[0] == nil)
        help_now! 'Missing label identifying the service to use.'
      else
        if (args.length > 1)
          help_now! "Too many arguments given for 'config use' command."
        else
          label = args[0]
          if ($tasc_conf.has_key?(label))
            $tasc_conf['current_server'] = label
            File.open(CFG_FILE, 'w') do |out|
              YAML.dump($tasc_conf, out)
            end
            ap "Switched connection to '#{label}'"
          else
            help_now! "Config label '#{label}' does not exist"
          end
        end

      end
    end
  end

  c.desc 'Adds a service connection'
  c.long_desc '[label] [server] arguments are required.  The label "current_server" is reserved. e.g., tasc add dev 127.0.0.1'
  c.command :add do |a|
    a.action do |global_options,options,args|
      if args.length == 2
        $tasc_conf[args[0]] = args[1]
        File.open(CFG_FILE, 'w') do |out|
          YAML.dump($tasc_conf, out)
        end
        ap "Configured new connection"
        ap "  '#{args[0]}' : '#{args[1]}'"
      else
        help_now! "Wrong number of arguments to 'config add'"
      end
    end
  end

  c.desc 'Delete a service connection named label'
  c.long_desc '[label] argument required.   The "current_server" label cannot be deleted. e.g., tasc delete dev'
  c.command :delete do |d|
    d.action do |global_options,options,args|
      if args.length == 1
        label = args[0]
        if ($tasc_conf.has_key?(label))
          $tasc_conf.delete(label)
          File.open(CFG_FILE, 'w') do |out|
            YAML.dump($tasc_conf, out)
          end
          ap "Deleted connection #{args[0]}"
        else
          help_now! "Service connection #{args[0]} does not exist."
        end
      else
        help_now! "Wrong number of arguments to 'config delete'"
      end
    end
  end

  c.default_command :list

end
