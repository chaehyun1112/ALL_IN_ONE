const fs = require('fs');
const zlib = require('zlib');
const b=fs.readFileSync(process.argv[2]);
const ss=1<<b.readUInt16LE(30), ms=1<<b.readUInt16LE(32);
const sec=i=>b.subarray((i+1)*ss,(i+2)*ss);
let dif=[];for(let i=0;i<109;i++){const x=b.readUInt32LE(76+i*4);if(x<0xfffffffa)dif.push(x)}
let fat=[];for(const i of dif){const s=sec(i);for(let j=0;j<ss;j+=4)fat.push(s.readUInt32LE(j))}
function chain(start,table,get){let a=[],seen=new Set();while(start<0xfffffffa&&!seen.has(start)){seen.add(start);a.push(get(start));start=table[start]}return Buffer.concat(a)}
const dir=chain(b.readUInt32LE(48),fat,sec), entries=[];
for(let p=0;p+128<=dir.length;p+=128){let n=dir.readUInt16LE(p+64);if(n>0)entries.push({name:dir.subarray(p,p+n-2).toString('utf16le'),type:dir[p+66],start:dir.readUInt32LE(p+116),size:Number(dir.readBigUInt64LE(p+120))})}
const root=entries.find(x=>x.type===5), mini=chain(root.start,fat,sec);
const mf=chain(b.readUInt32LE(60),fat,sec), mft=[];for(let i=0;i<mf.length;i+=4)mft.push(mf.readUInt32LE(i));
function stream(e){return (e.size<4096?chain(e.start,mft,i=>mini.subarray(i*ms,(i+1)*ms)):chain(e.start,fat,sec)).subarray(0,e.size)}
const head=stream(entries.find(x=>x.name==='FileHeader'));let out=[]; let cells=[]; let cell=null;
for(const e of entries.filter(x=>/^Section\d+$/.test(x.name))){let data=stream(e);if(head.readUInt32LE(36)&1)data=zlib.inflateRawSync(data);for(let p=0;p+4<=data.length;){const h=data.readUInt32LE(p);p+=4;const tag=h&1023;let len=h>>>20;if(len===4095){len=data.readUInt32LE(p);p+=4}const v=data.subarray(p,p+len);p+=len;if(tag===72){cell={header:v.toString("hex"),text:[]};cells.push(cell)} if(tag===67){let s='';for(let q=0;q+1<v.length;q+=2){const c=v.readUInt16LE(q);if([1,2,3,11,12,14,15,16,17,18,21,22,23,9].includes(c)){if(c===9)s+='\t';q+=14;}else if(c>=32)s+=String.fromCharCode(c);else if(c===13)s+='\n';}out.push(s);if(cell)cell.text.push(s)}}}
fs.writeFileSync(process.argv[3],out.join('\n'));

fs.writeFileSync(process.argv[3]+'.cells.json',JSON.stringify(cells,null,2));
