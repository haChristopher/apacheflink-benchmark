cd terraform

for i in $(terraform output -json instance_ips_producer | jq -r '.[]'); do
    scp -i ../ssh/client-key \
        -o "StrictHostKeyChecking no" \
        -o "UserKnownHostsFile=/dev/null" \
        -r provisioner@$i:~/messages/ \
        ../results/producer_$i
done

for i in $(terraform output -json instance_ips_consumers | jq -r '.[]'); do
    scp -i ../ssh/client-key \
        -o "StrictHostKeyChecking no" \
        -o "UserKnownHostsFile=/dev/null" \
        -r provisioner@$i:~/results/ \
        ../results/consumer_$i
done