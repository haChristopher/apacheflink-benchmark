"""
    This script distributes the settings from the configuration sh file to
    the terraform variables.tf and creates configuration files for bechmarking client.
"""
import re

# Read out conficuration from config.sh
with open('config.sh', 'r') as f:
    config = f.read()

    # Get the variables from the configuration
    variables = re.findall(r'([a-zA-Z_]+)=\"(.*)\"', config)

print("Found the following variables:")
print(variables)

# Replace variables in terraform file
with open('terraform/variables.tf', 'r') as f:
    terraform = f.read()

    for variable in variables:
        terraform = terraform.replace('"%s"' % variable[0], '"%s"' % variable[1])


# read file and save it to different location
with open('terraform/variables.tf', 'w') as f:
    f.write(terraform)
        