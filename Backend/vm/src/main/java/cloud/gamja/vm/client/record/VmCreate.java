package cloud.gamja.vm.client.record;

import cloud.gamja.vm.vms.enums.VmType;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VmCreate {
    private String node;
    private Integer vmid;
    private String name;
    private Integer cores;
    private String cpu;
    private Integer cpulimit;
    private Integer cpuunits;
    private String affinity;
    private String memory;
    private String ostype;
    private String pool;
    private String agent;
    private String scsihw;
    private String scsi0;
    private String net0;
    private String ide0;
    private String ide2;
    private String ciuser;
    private String sshkeys;
    private String ipconfig0;
    private String boot;

    public VmCreate(VmType vmtype) {
        switch (vmtype) {
            case FREE:
                this.node = "pve";
                this.cores = 4;
                this.cpu = "host,hidden=1";
                this.cpulimit = 4;
                this.cpuunits = 1024;
                this.affinity = "6-13";
                this.memory = "5120";
                this.ostype = "l26";
                this.pool = "user-vm";
                this.agent = "1";
                this.scsihw = "virtio-scsi-pci";
                this.net0 = "virtio,bridge=vmbr0";
                this.ide2 = "local-lvm:cloudinit";
                this.ipconfig0 = "ip=dhcp";
                this.boot = "order=scsi0;ide0";
                break;
            case PRO:
                this.node = "pve";
                this.cores = 8;
                this.cpu = "host,hidden=1";
                this.cpulimit = 0;
                this.cpuunits = 3072;
                this.affinity = "14-37";
                this.memory = "12288";
                this.ostype = "l26";
                this.pool = "user-vm";
                this.agent = "1";
                this.scsihw = "virtio-scsi-pci";
                this.net0 = "virtio,bridge=vmbr0";
                this.ide2 = "local-lvm:cloudinit";
                this.ipconfig0 = "ip=dhcp";
                this.boot = "order=scsi0;ide0";
                break;
        }
    }
}
