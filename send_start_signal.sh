cd terraform
touch start.txt

# Copy start signal file to each becnhmark client using scp
for i in $(terraform output -json instance_ips_producer | jq -r '.[]'); do
    scp -i ../ssh/client-key \
        -o "StrictHostKeyChecking no" \
        -o "UserKnownHostsFile=/dev/null" \
        -r start.txt \
        provisioner@$i:~/start.txt
done

# Copy start signal file to each becnhmark client using scp
for i in $(terraform output -json instance_ips_consumers | jq -r '.[]'); do
    scp -i ../ssh/client-key \
        -o "StrictHostKeyChecking no" \
        -o "UserKnownHostsFile=/dev/null" \
        -r start.txt \
        provisioner@$i:~/start.txt
done

rm start.txt