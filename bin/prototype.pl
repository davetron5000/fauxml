#!/usr/bin/perl
#

use strict;

die "usage $0 input output\n" if (!$ARGV[1]);

my $input = shift @ARGV;
my $output = shift @ARGV;

my $line_no = 0;

my %root = (
    "root" => 1,
    "depth" => -1,
    "children" => [],
    "attributes" => {},
);

my %variables = (
);
my $node = \%root;

open (INPUT,"$input") || die "Cannot open $input for reading: $!\n";
while (<INPUT>)
{
    chomp;
    $line_no++;
    next if (/^\s*#/);
    next if (/^\s*$/);
    if (/^\$/)
    {
        &handle_variable($_);
    }
    else
    {
        &handle_line($_);
    }
}
close (INPUT);

open (OUTPUT,">$output") || die "Cannot open $output for writing: $!\n";
print OUTPUT "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
my $child = $root{"children"};
foreach (@{$child})
{
    my $one_node = $_;
    &print_node(0,%{$one_node});
}
close(OUTPUT);

sub print_node
{
    my ($depth,%node) = @_;
    #my $depth = $node{"depth"};
    my $name = $node{"name"};
    my $text = $node{"text"};
    $text = &replace_variables($text);

    my $attributes = $node{"attributes"};
    my %attrib = %{$attributes} if ($attributes);

    print OUTPUT " " for(1..$depth);
    print OUTPUT "<$name";
    print OUTPUT " " if (%attrib);
    foreach (keys(%attrib))
    {
        print OUTPUT "$_=\"";
        print OUTPUT &replace_variables($attrib{$_});
        print OUTPUT "\" ";
    }
    my $children = $node{"children"};
    my @children = @{$children};
    if ( ($#children < 0) && (!$text || ($text eq "") || ($text =~ /^\n$/) ) )
    {
        print OUTPUT "/>\n" ;
    }
    else
    {
        print OUTPUT ">" ;
        print OUTPUT "$text";
        print OUTPUT "\n" if (!$text);

        foreach (@{$children})
        {
            &print_node($depth + 4,%{$_});
        }
        print OUTPUT " " for(1..$depth);
        print OUTPUT "</$name>\n";
    }
}
sub handle_line
{
    my ($line) = @_;

    my $space = $line;
    $line =~ s/^(\s*)//;
    $space =~ s/^(\s*).*$/$1/;

    if ($line =~ /^\|/)
    {
        $line = &escape($line);
        ${$node}{"text"} .= $line . "\n";
    }
    elsif ($line =~ /^([^=]+)=(.*)$/)
    {
        my $attributes = ${$node}{"attributes"};
        ${$attributes}{$1} = $2;
        #print STDERR "Adding $1 = $2 to " . ${$node}{"name"} . "\n";
    }
    elsif ($line =~ /^([\w\-:]+)\s*(.*)$/)
    {
        my $node_depth = ${$node}{"depth"};
        my $current_depth = length $space;
        my %new_node = 
        (
            "name" => $1,
            "depth" => $current_depth,
            "children" => [],
            "attributes" => {},
            "text" => $2 . "\n",
        );
        if ($current_depth < $node_depth)
        {
            #print STDERR "  $new_node{'name'} is an uncle or something of ${$node}{'name'}\n";
            # We must find the parent of nodes of this depth
            my $new_parent = $node;
            while (${$new_parent}{"depth"} >= $current_depth)
            {
                $new_parent = ${$new_parent}{"parent"};
                #print STDERR "    ${$new_parent}{'name'} is being checked (depth ${$new_parent}{'depth'} vs. $current_depth)\n";
                last if (!$new_parent);
            }
            if ($new_parent)
            {
                my $children = ${$new_parent}{"children"};
                $new_node{"parent"} = $new_parent;
                push @{$children},\%new_node;
                $node = \%new_node;
            }
        }
        elsif ($current_depth == $node_depth)
        {
            #print STDERR "  $new_node{'name'} is a sibling of ${$node}{'name'}\n";
            # New node is sibling of current node
            my $parent = ${$node}{"parent"};
            my $siblings = ${$parent}{"children"};
            $new_node{"parent"} = $parent;
            push @{$siblings},\%new_node;
            $node = \%new_node;
        }
        else
        {
            #print STDERR "  $new_node{'name'} is a child of ${$node}{'name'}\n";
            #New node is child of current node
            my $children = ${$node}{"children"};
            $new_node{"parent"} = $node;
            push @{$children},\%new_node;
            $node = \%new_node;
        }
    }
    else
    {
        #print STDERR "No clue how to deal with line $line_no ($line)\n"
    }


}


sub escape
{
    my ($line) = @_;

    $line =~ s/^\|//;
    $line =~ s/\&/\&amp;/g;
    $line =~ s/\</\&lt;/g;
    $line =~ s/\>/\&gt;/g;

    return $line;
}

sub handle_variable
{
    my ($line) = @_;

    my ($var,$val) = split(/=/,$line,2);
    $var = &trim($var);
    $val = &trim($val);
    $val = &replace_variables($val);
    $variables{$var} = $val;
}

sub replace_variables
{
    my ($value) = @_;

    foreach my $var (keys(%variables))
    {
        my $val = $variables{$var};
        my $quoted_var = $var;
        $quoted_var =~ s/^\$//;

        $var =~ s/\$/\\\$/g;

        $value =~ s/\/\$/\${}/g;
        $quoted_var = "\\\${" . $quoted_var . "}"; 
        $value =~ s/$quoted_var/$val/g;
        $value =~ s/$var$/$val/g;
        $value =~ s/$var([^\w\d])/$val$1/g;
        $value =~ s/\${}/\$/g;
    }

    if ( ($value =~ /^\$/)  || ($value =~ /[^\\]\$/) )
    {

        print STDERR "$value on line $line_no has undeclared variables\n";
    }

    return $value;

}

sub trim
{
    my ($trim) = @_;

    $trim =~ s/^\s*//g;
    $trim =~ s/\s*$//g;

    return $trim;
}
