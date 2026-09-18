"use strict";
// [2026.09.17] 추가한 내용: 스크립트 위치를 기준으로 서버와 Live Server에서 동일한 호실 WAV 폴더를 사용합니다.
const roomAlertAudioBase = new URL("../../audio/room-alerts/", document.currentScript.src);
document.addEventListener("DOMContentLoaded", () => {
  /* [2026.09.16] 추가한 내용: 버튼으로 전체화면을 전환하고 Esc 해제 시에도 버튼 상태를 동기화합니다. */
  const fullscreenToggle = document.getElementById("fullscreen-toggle");
  const fullscreenLabel = document.getElementById("fullscreen-label");
  function syncFullscreenButton() {
    const isFullscreen = Boolean(document.fullscreenElement);
    const label = isFullscreen ? "전체화면 해제" : "전체화면";
    fullscreenToggle.setAttribute("aria-pressed", String(isFullscreen));
    fullscreenToggle.setAttribute("aria-label", label);
    fullscreenToggle.title = label;
    fullscreenLabel.textContent = label;
  }
  document.addEventListener("fullscreenchange", syncFullscreenButton);
  syncFullscreenButton();
  if (!document.fullscreenEnabled) {
    fullscreenToggle.disabled = true;
    fullscreenToggle.title = "이 브라우저에서는 전체화면을 사용할 수 없습니다.";
  }
  fullscreenToggle.addEventListener("click", async () => {
    fullscreenToggle.disabled = true;
    try {
      if (document.fullscreenElement) {
        await document.exitFullscreen();
      } else {
        await document.documentElement.requestFullscreen();
      }
    } catch {
      window.alert("전체화면 전환에 실패했습니다. 브라우저의 전체화면 권한을 확인해 주세요.");
    } finally {
      fullscreenToggle.disabled = !document.fullscreenEnabled;
      syncFullscreenButton();
    }
  });

  /* [수정] 화면 예시 상태입니다. 실제 서버 조회 결과로 교체하세요.
     페이지를 열 때 조회한 이전 경보에는 음성을 재생하지 않습니다. */
  const rooms = window.CareGuardRoomStatus.rooms;
  const labels = window.CareGuardRoomStatus.labels;
  const upper=document.getElementById("upper-rooms"), lower=document.getElementById("lower-rooms");
  const detail=document.getElementById("room-detail");
  const dialog=document.getElementById("response-dialog"), form=document.getElementById("response-form");
  const alertPanel=document.querySelector(".alerts-panel");
  const corridorNode=document.getElementById("central-corridor");
  // [2026.09.16] 추가한 내용: 환자를 특정하지 않아도 위치만으로 표시할 수 있는 복도 낙상 경보 예시입니다.
  // [2026.09.16] 고친 내용: 복도 감지 위치를 화면 방향 대신 고정 방위 표기인 서·화장실 앞으로 표시합니다.
  const corridorAlert={location:"중앙 복도",cameraId:"C-02",cameraLocation:"서·화장실 앞",status:"urgent",acknowledged:false,eventId:"corridor-fall-001",occurredAt:"2026-09-16T14:33:00+09:00"};
  let selected=null, corridorSelected=false, filter="all";
  // [2026.09.17] 추가한 내용: 새로 수신한 감지만 공통 상단 배너에 보관하며 기존 예시 알림은 자동으로 띄우지 않습니다.
  const crossPageAlert=document.getElementById('cross-page-alert');
  const crossPageItems=document.getElementById('cross-page-alert-items');
  const crossPageToggle=document.getElementById('cross-page-alert-toggle');
  const pendingNotices=new Map();
  let noticeSignature='',noticeCollapseTimer=null;
  let primaryNoticeKey=null;
  function setNoticeExpanded(expanded){
    crossPageItems.hidden=!expanded;
    crossPageToggle.setAttribute('aria-expanded',String(expanded));
    crossPageToggle.textContent=expanded?'접기':'펼치기';
  }
  // [2026.09.17] 고친 내용: 접힌 제목과 창의 빈 공간까지 한 곳에서 클릭을 처리하고 배경 선택 해제 이벤트로 전파하지 않습니다.
  crossPageAlert.addEventListener('click',event=>{
    event.stopPropagation();
    if(event.target.closest('#cross-page-alert-toggle')){
      clearTimeout(noticeCollapseTimer);
      setNoticeExpanded(crossPageToggle.getAttribute('aria-expanded')!=='true');return;
    }
    const row=event.target.closest('.cross-page-alert-row');
    const key=row?(row.dataset.location==='corridor'?'corridor':Number(row.dataset.location)):primaryNoticeKey;
    if(key===null)return;
    if(event.target.closest('[data-action="respond"]')){openResponseRegistration(key);return;}
    setDashboardView('dashboard');
    // 클릭한 위치를 직접 선택하여 이전 선택 상태나 별도 선택 모듈에 영향받지 않게 합니다.
    corridorSelected=key==='corridor';selected=corridorSelected?null:key;render();
    const node=corridorSelected?corridorNode:nodes.get(key);
    node?.scrollIntoView({block:'center',behavior:'smooth'});node?.focus({preventScroll:true});
  });
  function rememberNotice(key){
    const state=key==='corridor'?corridorAlert:rooms.get(key);
    if(state)pendingNotices.set(key,{id:state.eventId,receivedAt:Date.now()});
  }
  function renderCrossPageAlert(){
    const notices=[];
    for(const [key,notice] of pendingNotices){
      const state=key==='corridor'?corridorAlert:rooms.get(key);
      if(!state||state.status==='normal'||state.acknowledged||state.eventId!==notice.id){pendingNotices.delete(key);continue;}
      notices.push({key,state,...notice});
    }
    if(document.body.dataset.dashboardView!=='records'||!notices.length){
      crossPageAlert.hidden=true;noticeSignature='';clearTimeout(noticeCollapseTimer);return;
    }
    notices.sort((a,b)=>Number(a.state.status==='caution')-Number(b.state.status==='caution')||a.receivedAt-b.receivedAt);
    primaryNoticeKey=notices[0].key;
    const signature=notices.map(notice=>notice.id).join('|');
    if(signature===noticeSignature&&!crossPageAlert.hidden)return;
    noticeSignature=signature;
    document.getElementById('cross-page-alert-summary').textContent=`미처리 감지 알림 ${notices.length}건`;
    crossPageItems.replaceChildren();
    for(const notice of notices){
      const row=document.createElement('div');row.className='cross-page-alert-row '+notice.state.status;
      row.dataset.location=String(notice.key);
      // [2026.09.17] 고친 내용: 알림 문구도 키보드로 선택할 수 있는 대시보드 이동 버튼으로 제공합니다.
      const title=document.createElement('button');title.type='button';title.className='cross-page-alert-location';
      const location=notice.key==='corridor'?notice.state.cameraLocation:`${notice.key}호`;
      title.textContent=`${notice.state.test?'[테스트] ':''}${location} · ${labels[notice.state.status]}`;
      const response=document.createElement('button');response.type='button';response.textContent='대응 등록';
      response.dataset.action='respond';
      const locate=document.createElement('button');locate.type='button';locate.textContent='위치 보기';
      row.append(title,response,locate);crossPageItems.append(row);
    }
    crossPageAlert.classList.toggle('caution',notices[0].state.status==='caution');
    crossPageAlert.hidden=false;setNoticeExpanded(true);
    // [2026.09.17] 추가한 내용: 읽던 화면을 가리지 않도록 8초 뒤 건수 표시로 접고 미처리 알림은 유지합니다.
    clearTimeout(noticeCollapseTimer);noticeCollapseTimer=setTimeout(()=>setNoticeExpanded(false),8000);
  }
  /* [추가] 전체 화면 상태와 ‘전체’ 카드의 선택 테두리를 분리해 관리합니다. */
  let cardSelected=false;
  /* [추가] 클릭 직후에는 커서를 빼기 전까지 버튼 hover 강조를 표시하지 않습니다. */
  let suppressedHoverRoom=null;
  const nodes=new Map();
  upper.replaceChildren();lower.replaceChildren();
  function addRoom(number,parent){
    const node=document.createElement("button");node.type="button";node.className="room";
    node.innerHTML=`<strong>${number}호</strong><small></small><span class="door" aria-hidden="true"></span>`;
    node.addEventListener("click",()=>choose(number));nodes.set(number,node);parent.append(node);
  }
  function facility(){const el=document.createElement("div");el.className="facility";el.innerHTML='<span class="symbol" aria-hidden="true">WC</span><span>화장실</span>';return el;}
  for(let n=301;n<=310;n++)addRoom(n,upper);
  lower.append(facility());for(let n=311;n<=313;n++)addRoom(n,lower);
  const gap=document.createElement("div");gap.setAttribute("aria-hidden","true");lower.append(gap);
  for(let n=314;n<=317;n++)addRoom(n,lower);lower.append(facility());

  /* [2026.09.16] 고친 내용: 현황 카드를 전체·낙상·침대 이탈·정상 순서로 표시해도 기존 상태 필터와 연결합니다. */
  const cards=[];
  document.querySelectorAll(".counts .count").forEach(old=>{
    const button=document.createElement("button");button.type="button";button.className=old.className;
    button.append(...old.childNodes);old.replaceWith(button);
    const kind=button.classList.contains("all")?"all":button.classList.contains("urgent")?"urgent":button.classList.contains("caution")?"caution":"normal";
    button.dataset.filter=kind;button.addEventListener("click",()=>{filter=kind;cardSelected=true;render();});cards.push(button);
  });
  const filterInfo=document.createElement("div");filterInfo.className="filter-info";
  /* [수정] 별도 전체 보기 버튼은 제외합니다. 선택한 카드를 다시 누르면 해제됩니다. */
  filterInfo.innerHTML='<span role="status" aria-live="polite"></span>';
  /* [수정] 강조 상태 안내는 화면에 추가하지 않습니다. */

  /* [2026.09.17] 고친 내용: 낙상과 침대 이탈에 같은 호실별 WAV를 사용하며 종료 후 대기 없이 최대 5회 재생합니다.
     대기열은 경보별로 관리하며 한 번에 하나만 재생합니다. */
  const audio=new Audio(), jobs=new Map(), seenEvents=new Set();
  const audioControls=document.createElement("div");audioControls.className="audio-controls";
  /* [수정] 음성 켜기 체크박스 없이 재생 상태만 안내합니다. */
  audioControls.innerHTML='<span role="status" aria-live="polite">새 낙상 또는 침대 이탈 발생 시 음성으로 안내합니다.</span>';
  /* [수정] 음성 안내 문구는 화면에 추가하지 않습니다. 재생 기능은 유지합니다. */
  let audioAllowed=true;
  // [2026.09.17] 추가한 내용: 테스트 모드에서는 재생 성공·차단·중지 상태를 직접 확인합니다.
  const testMode=new URLSearchParams(window.location.search).get("testAlerts")==="1";
  const soundInfo=testMode ? document.getElementById("alert-test-audio-status") : audioControls.querySelector("span");
  let queue=[],playing=null,epoch=0;
  function cancelAudio(number){
    const job=jobs.get(number);if(job){job.cancelled=true;jobs.delete(number);}
    queue=queue.filter(j=>j.number!==number);
    if(playing?.number===number){epoch++;playing=null;audio.pause();audio.removeAttribute("src");audio.load();}
    pump();
  }
  async function pump(){
    if(playing||!audioAllowed)return;
    while(queue.length&&queue[0].cancelled)queue.shift();
    if(!queue.length)return;
    const job=queue.shift();playing=job;const token=++epoch;
    audio.src=new URL(job.fileName,roomAlertAudioBase).href;
    try{await audio.play();if(token===epoch){job.count++;soundInfo.textContent=`${job.label} 음성 ${job.count}/5회 재생 중`;}}
    catch(error){
      if(token!==epoch)return;
      playing=null;
      if(error.name==="NotAllowedError"){
        queue.unshift(job);audioAllowed=false;
        soundInfo.textContent="음성을 재생하려면 화면을 한 번 클릭해주세요.";
      }else{job.cancelled=true;jobs.delete(job.number);soundInfo.textContent=`${job.label} 음성 파일을 확인해주세요.`;pump();}
    }
  }
  audio.addEventListener("ended",()=>{
    const job=playing;playing=null;if(!job||job.cancelled)return;
    if(job.count<5){
      // [2026.09.17] 고친 내용: 기존 3초 대기를 제거하고 종료 즉시 다음 안내를 재생합니다.
      queue.push(job);
    }else{jobs.delete(job.number);soundInfo.textContent=`${job.label} 5회 안내 완료`;}
    pump();
  });
  /* [수정] 브라우저가 음성을 차단하면 다음 실제 클릭/키 입력에서 대기 경보를 재시도합니다. */
  function resumeAudio(){audioAllowed=true;pump();}
  document.addEventListener("click",resumeAudio);
  document.addEventListener("keydown",resumeAudio);
  // [2026.09.17] 추가한 내용: 두 감지 유형이 같은 호실 안내와 반복 취소 처리를 공유합니다.
  function queueRoomAudio(number, fileName=`${number}호즉시확인.wav`, label=`${number}호`){
    cancelAudio(number);
    const job={number,fileName,label,count:0,cancelled:false};
    jobs.set(number,job);queue.push(job);pump();
  }

  // [09.13]수정내용: 병실 선택 처리를 전용 모듈에 위임하고 현재 화면 상태만 전달합니다.
  function choose(number){corridorSelected=false;window.CareGuardRoomSelection.choose({get selected(){return selected;},set selected(value){selected=value;}}, number, render);}
  function chooseCorridor(){selected=null;corridorSelected=true;render();corridorNode.focus({preventScroll:true});}
  corridorNode.addEventListener("click",chooseCorridor);
  /* [추가] 전체/상태 카드가 아닌 화면을 클릭하면 카드 선택 테두리를 제거합니다. */
  document.addEventListener("click", event => {
    if (event.target.closest(".counts .count")) return;
    if (filter === "all" && !cardSelected) return;
    filter = "all";
    cardSelected=false;
    render();
  });
  /* [추가] 빈 배경 클릭 시 병실 선택을 해제합니다.
     버튼·입력칸·등록 창 조작은 선택을 유지합니다. 경보 상태는 변경하지 않습니다. */
  document.addEventListener("click",event=>{
    if(event.target.closest("button,a,input,select,textarea,label,dialog,[role='button'],.dashboard-history-card"))return;
    /* [수정] 상단 카드 선택도 빈 화면 클릭 시 함께 해제합니다. */
    if(selected===null && !corridorSelected && filter==="all")return;
    selected=null;
    corridorSelected=false;
    filter="all";
    cardSelected=false;
    render();
  });
  function renderRoomHistory(){
    const room=rooms.get(selected);
    // [2026.09.16] 고친 내용: 가로형 기록 카드에 병실·건수·최근 발생 시각을 표시하며 미선택 상태는 대시로 구분합니다.
    const now=Date.now();
    const counts=room ? window.CareGuardRoomStatus.todayCounts(room.number,now) : null;
    const latest=room ? window.CareGuardRoomStatus.latestTodayEvent(room.number,now) : null;
    detail.innerHTML='<div class="history-heading"><strong class="history-room"></strong><span>오늘의 기록</span></div>'+
      '<div class="history-metric"><span class="room-history-urgent">낙상 감지</span><div><strong class="history-fall"></strong><span>건</span></div></div>'+
      '<div class="history-metric"><span class="room-history-caution">침대 이탈</span><div><strong class="history-exit"></strong><span>건</span></div></div>'+
      '<div class="history-recent"><span>최근 기록</span><strong class="history-event"></strong><time></time></div>';
    // [2026.09.17] 고친 내용: 복도 테스트 종류와 실제 선택 위치를 하단 기록에도 동일하게 표시합니다.
    detail.querySelector('.history-room').textContent=corridorSelected ? corridorAlert.cameraLocation : room ? `${room.number}호` : '병실 또는 복도를 선택해 주세요';
    const corridorEventType=corridorAlert.eventType || 'urgent';
    detail.querySelector('.history-fall').textContent=corridorSelected ? String(corridorEventType==='urgent' ? 1 : 0) : counts ? counts.urgent : '—';
    detail.querySelector('.history-exit').textContent=corridorSelected ? String(corridorEventType==='caution' ? 1 : 0) : counts ? counts.caution : '—';
    detail.querySelector('.history-event').textContent=corridorSelected ? `${corridorAlert.test ? '테스트 · ' : ''}${labels[corridorEventType]} · ${corridorAlert.cameraLocation}` : latest ? labels[latest.type] : room ? '오늘 감지된 이벤트가 없습니다.' : '선택한 위치의 기록을 표시합니다.';
    const time=detail.querySelector('time');
    const occurredAt=corridorSelected ? corridorAlert.occurredAt : latest?.occurredAt;
    if(occurredAt){
      time.dateTime=new Date(occurredAt).toISOString();
      time.textContent=new Intl.DateTimeFormat('ko-KR',{timeZone:'Asia/Seoul',hour:'2-digit',minute:'2-digit',hour12:false}).format(new Date(occurredAt));
    }else time.hidden=true;
  }
  function render(){
    const counts={all:rooms.size+1,normal:0,caution:0,urgent:0};
    // [2026.09.16] 고친 내용: 하단 카드는 renderRoomHistory에서 선택 병실의 오늘 기록만 표시합니다.
    for(const room of rooms.values()){
      counts[room.status]++;const node=nodes.get(room.number);
      node.classList.toggle("urgent",room.status==="urgent");node.classList.toggle("caution",room.status==="caution");
      node.classList.toggle("acknowledged",room.acknowledged);
      node.classList.toggle("filtered-out",filter!=="all"&&room.status!==filter&&room.status!=="urgent");
      /* [수정] 병실 카드에는 상태명만 표시하고 ‘확인 중’ 문구는 숨깁니다. */
      node.querySelector("small").textContent=labels[room.status];
      node.setAttribute("aria-label",`${room.number}호 · ${node.querySelector("small").textContent}`);
      node.setAttribute("aria-pressed",String(selected===room.number));
    }
    counts[corridorAlert.status]++;
    corridorNode.classList.toggle("urgent",corridorAlert.status==="urgent");
    corridorNode.classList.toggle("caution",corridorAlert.status==="caution");
    corridorNode.style.setProperty("--camera-x",corridorAlert.cameraX || "13%");
    corridorNode.setAttribute("aria-label",`${corridorAlert.cameraLocation} · ${labels[corridorAlert.status]}`);
    corridorNode.style.setProperty("--camera-label", `"${corridorAlert.cameraLocation}"`);
    corridorNode.classList.toggle("acknowledged",corridorAlert.acknowledged);
    corridorNode.setAttribute("aria-pressed",String(corridorSelected));
    cards.forEach(card=>{card.querySelector("b").textContent=counts[card.dataset.filter];card.setAttribute("aria-pressed",String(cardSelected&&filter===card.dataset.filter));});
    filterInfo.querySelector("span").textContent=filter==="all"?"전체 병실 표시 중":`${{normal:"정상 병실",caution:"침대 이탈",urgent:"낙상"}[filter]} 강조 중 · 낙상 병실은 항상 표시`;
    renderRoomHistory();
    alertPanel.querySelectorAll(".alert-card").forEach(el=>el.remove());
    for(const room of rooms.values()){
      if(room.status==="normal")continue;
      const card=document.createElement("article");card.className="alert-card"+(room.status==="caution"?" caution":"");
      card.innerHTML=`<div class="alert-top"><strong>● ${room.status==="urgent"?"긴급":"주의"}</strong></div><h3>${room.number}호 <span class="event-label">${labels[room.status]}</span></h3><p>병실을 확인해주세요.</p><button type="button" class="locate-room">대응 등록</button>`;
      // [2026.09.17] 추가한 내용: 가상 감지 카드에는 테스트 표시를 붙입니다.
      if(room.test)card.querySelector('.alert-top').insertAdjacentHTML('beforeend','<span class="test-alert-badge">테스트</span>');
      /* [2026.09.17] 고친 내용: 알림 카드의 대응 등록 버튼은 해당 병실을 선택하고 등록 창을 바로 엽니다. */
      card.querySelector("button").setAttribute("aria-pressed",String(selected===room.number));
      card.querySelector("button").classList.toggle("hover-suppressed",suppressedHoverRoom===room.number);
      card.querySelector("button").addEventListener("mouseleave",event=>{
        if(suppressedHoverRoom===room.number)suppressedHoverRoom=null;
        event.currentTarget.classList.remove("hover-suppressed");
      });
      // [2026.09.17] 고친 내용: 중간 화면 재생성이나 이전 선택 상태에 의존하지 않고 버튼의 병실 번호를 등록 창에 직접 전달합니다.
      card.querySelector("button").addEventListener("click",()=>{suppressedHoverRoom=room.number;openResponseRegistration(room.number);});alertPanel.append(card);
    }
    if(corridorAlert.status!=="normal"){
      const card=document.createElement("article");card.className="alert-card corridor-alert"+(corridorAlert.status==="caution"?" caution":"");
      // [2026.09.16] 고친 내용: 복도 알림에서는 넓은 구역명보다 실제 발생 위치를 제목으로 크게 표시합니다.
      card.innerHTML=`<div class="alert-top"><strong>● ${corridorAlert.status==="urgent"?"긴급":"주의"}</strong>${corridorAlert.test?'<span class="test-alert-badge">테스트</span>':''}</div><h3>${corridorAlert.cameraLocation} <span class="event-label">${labels[corridorAlert.status]}</span></h3><p>발생 구역 · ${corridorAlert.location}</p><button type="button" class="locate-room">대응 등록</button>`;
      card.querySelector("button").setAttribute("aria-pressed",String(corridorSelected));
      card.querySelector("button").addEventListener("click",()=>openResponseRegistration('corridor'));alertPanel.append(card);
    }
    // [2026.09.17] 고친 내용: 테스트 위치를 포함한 모든 낙상 알림을 침대 이탈보다 먼저 표시합니다.
    [...alertPanel.querySelectorAll('.alert-card')].sort((a,b)=>Number(a.classList.contains('caution'))-Number(b.classList.contains('caution'))).forEach(card=>alertPanel.append(card));
    document.getElementById("alert-total").textContent=`${counts.urgent+counts.caution}건`;
    renderCrossPageAlert();
  }

  /* [추가] 실제 SSE/WebSocket 수신부에서 아래 함수를 호출하세요.
     window.CareGuard.receiveFallEvent({ id: 서버의_고유_경보ID, room: 308 });
     동일 경보 ID 재수신은 무시합니다. 저장·통신 API는 이 파일에 포함하지 않습니다. */
  window.CareGuard={receiveFallEvent(event){
    if(!event||event.id==null||!String(event.id).trim()||!rooms.has(Number(event.room)))return false;
    const id=String(event.id);if(seenEvents.has(id)||!window.CareGuardRoomStatus.addEvent({...event,type:"urgent"}))return false;seenEvents.add(id);
    const number=Number(event.room);
    Object.assign(rooms.get(number),{status:"urgent",acknowledged:false, eventId:id,test:Boolean(event.test)});
    rememberNotice(number);render();queueRoomAudio(number);return true;
  },receiveBedExitEvent(event){
    if(!event||!window.CareGuardRoomStatus.addEvent({...event,type:"caution"}))return false;
    const room=rooms.get(Number(event.room));
    // 같은 병실의 미해결 낙상 경보를 주의 상태로 낮추지 않습니다.
    if(room.status!=="urgent")Object.assign(room,{status:"caution",acknowledged:false,eventId:String(event.id),test:Boolean(event.test)});
    // [2026.09.17] 추가한 내용: 침대 이탈도 화면 알림 갱신과 함께 해당 호실 음성을 재생합니다.
    rememberNotice(Number(event.room));render();queueRoomAudio(Number(event.room));return true;
  }};

  let registrationRoom=null, registrationEvent=null;
  // [2026.09.17] 추가한 내용: 주소로 켠 테스트 모드에서만 가상 감지를 주입하며 실제 통신이나 DB 저장을 실행하지 않습니다.
  if(testMode){
    document.body.classList.add('alert-test-mode');
    const panel=document.getElementById('alert-test-panel');
    const locationSelect=document.getElementById('alert-test-location');
    const typeSelect=document.getElementById('alert-test-type');
    const result=document.getElementById('alert-test-result');
    const originalRooms=new Map();
    const originalCorridor={...corridorAlert};
    const testIds=new Set();
    let delayedTestTimer=null;
    const corridorLocations={
      'wc-301':{cameraLocation:'301호 화장실 앞',cameraX:'5%',fileName:'301호화장실앞즉시확인.wav'},
      'wc-310':{cameraLocation:'310호 화장실 앞',cameraX:'95%',fileName:'310호화장실앞즉시확인.wav'}
    };
    for(const number of rooms.keys())locationSelect.add(new Option(`${number}호`,String(number)));
    for(const [id,location] of Object.entries(corridorLocations))locationSelect.add(new Option(location.cameraLocation,id));
    panel.hidden=false;
    audio.id='alert-test-audio';audio.hidden=true;panel.append(audio);
    function runTestAlert(location,type){
      const locationLabel=[...locationSelect.options].find(option=>option.value===location)?.textContent;
      const id=`test-${Date.now()}-${crypto.randomUUID()}`;
      if(corridorLocations[location]){
        // 현재 도면의 단일 복도 표시를 재사용하므로 이전 복도 음성은 먼저 취소합니다.
        cancelAudio('corridor');
        Object.assign(corridorAlert,corridorLocations[location],{status:type,eventType:type,test:true,eventId:id,occurredAt:new Date().toISOString(),acknowledged:false});
        rememberNotice('corridor');render();queueRoomAudio('corridor',corridorAlert.fileName,corridorAlert.cameraLocation);
      }else{
        const number=Number(location),room=rooms.get(number);
        if(!room)return;
        if(type==='caution'&&room.status==='urgent'){
          result.textContent='현재 낙상 알림이 있는 병실입니다. 정상 병실을 선택하거나 테스트 초기화 후 진행해 주세요.';return;
        }
        if(!originalRooms.has(number))originalRooms.set(number,{...room});
        const receive=type==='urgent'?window.CareGuard.receiveFallEvent:window.CareGuard.receiveBedExitEvent;
        if(!receive({id,room:number,test:true,occurredAt:Date.now()}))return;
      }
      testIds.add(id);
      result.textContent=`테스트 발생 · ${locationLabel} · ${labels[type]}`;
    }
    document.getElementById('alert-test-run').addEventListener('click',()=>runTestAlert(locationSelect.value,typeSelect.value));
    // [2026.09.17] 추가한 내용: 선택한 값을 예약 시점에 저장하여 페이지 이동 후에도 동일한 가상 감지가 발생합니다.
    document.getElementById('alert-test-delayed').addEventListener('click',()=>{
      clearTimeout(delayedTestTimer);
      const location=locationSelect.value,type=typeSelect.value;
      result.textContent='5초 후 테스트 알림이 발생합니다. 사용자 메뉴에서 조치기록으로 이동해 주세요.';
      delayedTestTimer=setTimeout(()=>{delayedTestTimer=null;runTestAlert(location,type);},5000);
    });
    // [2026.09.17] 추가한 내용: 테스트가 바꾼 위치만 원상 복구하고 테스트 기록·반복 타이머를 제거합니다.
    document.getElementById('alert-test-reset').addEventListener('click',()=>{
      clearTimeout(delayedTestTimer);delayedTestTimer=null;
      for(const [number,original] of originalRooms){
        const room=rooms.get(number);
        if(!room.test)continue;
        cancelAudio(number);Object.assign(room,original,{test:false});
      }
      if(corridorAlert.test){
        cancelAudio('corridor');Object.assign(corridorAlert,originalCorridor,{test:false,cameraX:originalCorridor.cameraX,eventType:originalCorridor.eventType});
      }
      window.CareGuardRoomStatus.clearTestEvents();
      for(const id of testIds)seenEvents.delete(id);
      testIds.clear();originalRooms.clear();
      if(dialog.open&&testMode)dialog.close();
      registrationRoom=null;registrationEvent=null;
      render();result.textContent='테스트 초기화 완료 · 기존 알림은 유지됩니다.';
      soundInfo.textContent='테스트 음성과 남은 반복을 중지했습니다.';
    });
    window.addEventListener('pagehide',()=>clearTimeout(delayedTestTimer));
  }
  // [2026.09.17] 고친 내용: 알림 카드에서만 대응 등록 창을 열어 범례 아래의 중복 버튼을 제거합니다.
  // [2026.09.17] 고친 내용: 305호·312호와 복도 모두 클릭한 위치를 명시적으로 선택한 뒤 한 번만 화면을 갱신합니다.
  function openResponseRegistration(location){
    corridorSelected=location==='corridor';
    selected=corridorSelected?null:Number(location);
    const room=corridorSelected ? corridorAlert : rooms.get(selected);if(!room||room.status==="normal")return;
    registrationRoom=corridorSelected ? "corridor" : selected;registrationEvent=room.eventId;room.acknowledged=true;
    // [2026.09.17] 고친 내용: 복도에서도 대응 등록 클릭 즉시 음성과 남은 반복을 중지합니다.
    cancelAudio(corridorSelected ? "corridor" : selected);
    soundInfo.textContent=`${corridorSelected ? corridorAlert.cameraLocation : `${selected}호`} 대응 시작 · 음성과 남은 반복 중지`;
    form.reset();document.getElementById("response-title").textContent=`${labels[room.status]} 대응 등록`;
    const eventBox=document.getElementById("response-event");
    eventBox.textContent=`${room.test?'테스트 · ':''}${room.cameraLocation ?? `${selected}호`} · ${labels[room.status]}`;
    /* [추가] 침대 이탈 등록 창은 주의 색상 클래스를 적용합니다. */
    eventBox.classList.toggle("caution-event",room.status==="caution");
    eventBox.classList.toggle("urgent-event",room.status==="urgent");
    // [2026.09.17] 추가한 내용: 낙상 감지 대응 등록 창 전체를 긴급 테두리로 구분합니다.
    dialog.classList.toggle("urgent-response-dialog",room.status==="urgent");
    render();if(!dialog.open)dialog.showModal();
  }
  document.getElementById("response-cancel").addEventListener("click",()=>dialog.close());
  /* [수정] 대응 내용을 서버에 저장할 위치입니다. 현재는 화면에만 반영됩니다. */
  form.addEventListener("submit",event=>{
    event.preventDefault();const room=registrationRoom==="corridor" ? corridorAlert : rooms.get(registrationRoom);if(!room)return;
    /* [추가] 등록 창을 연 뒤 같은 병실에 새 경보가 오면 이전 등록으로 해제하지 않습니다. */
    if(room.eventId!==registrationEvent){dialog.close();detail.textContent="새 낙상이 발생했습니다. 해당 병실의 대응 등록을 다시 열어주세요.";return;}
    // 조치 내용 등록은 조치 완료로 처리합니다.
    // 실제 적용 시 이 위치에서 서버 저장 성공을 확인한 후 상태를 변경하세요.
    room.status="normal";room.acknowledged=false;if(registrationRoom!=="corridor")cancelAudio(room.number);
    dialog.close();render();
  });
  const toggle=document.getElementById("profile-toggle"),menu=document.getElementById("header-menu-list");
  /* [2026.09.17] 고친 내용: 사용자 프로필 메뉴에서 조치기록·비밀번호 변경·로그아웃을 제공합니다. */
  function closeMenu(){menu.hidden=true;toggle.setAttribute("aria-expanded","false");}
  // [2026.09.17] 추가한 내용: 비밀번호 변경 링크를 팝업으로 열고 닫을 때 입력 문서를 제거합니다.
  const passwordLink=document.getElementById('password-change-open');
  const passwordDialog=document.getElementById('password-change-dialog');
  const passwordFrame=document.getElementById('password-change-frame');
  passwordLink.addEventListener('click',event=>{
    event.preventDefault();closeMenu();
    passwordFrame.src=passwordLink.href;
    if(!passwordDialog.open)passwordDialog.showModal();
  });
  document.getElementById('password-popup-close').addEventListener('click',()=>passwordDialog.close());
  passwordDialog.addEventListener('close',()=>{passwordFrame.src='about:blank';toggle.focus();});
  // [2026.09.17] 추가한 내용: 같은 출처의 비밀번호 입력 프레임이 보낸 완료·닫기 요청만 처리합니다.
  window.addEventListener('message',event=>{
    if(event.origin!==window.location.origin||event.source!==passwordFrame.contentWindow||!passwordDialog.open)return;
    if(event.data?.type==='careguard-password-close')passwordDialog.close();
    if(event.data?.type==='careguard-password-changed'){
      // [2026-09-18] 변경 팝업을 배경에 유지하고 완료 안내의 확인 버튼을 누른 뒤 팝업을 닫는다.
      window.alert('비밀번호 변경이 완료되었습니다');passwordDialog.close();
    }
  });
  toggle.addEventListener("click",()=>{menu.hidden=!menu.hidden;toggle.setAttribute("aria-expanded",String(!menu.hidden));});
  document.addEventListener("click",e=>{if(!e.target.closest(".header-menu"))closeMenu();});
  document.addEventListener("keydown",e=>{if(e.key==="Escape")closeMenu();});
  // [2026.09.17] 추가한 내용: 조치기록을 같은 대시보드 문서에서 열어 전체화면을 유지합니다.
  const dashboardMain=document.getElementById("dashboard-main"),recordView=document.getElementById("dashboard-record-view"),recordFrame=document.getElementById("dashboard-record-frame"),recordOpen=document.getElementById("record-open");
  const dashboardBack=document.querySelector('.dashboard-back-link');
  function setDashboardView(view,updateAddress=true){
    const isRecords=view==="records";
    document.body.dataset.dashboardView=view;
    dashboardMain.hidden=isRecords;recordView.hidden=!isRecords;
    if(isRecords&&!recordFrame.dataset.loaded){recordFrame.src=recordFrame.dataset.src;recordFrame.dataset.loaded="true";}
    // [2026.09.17] 고친 내용: 화면 전환과 배너 숨김을 함께 적용하고 테스트 모드 주소를 유지합니다.
    if(updateAddress){
      const target=new URL(isRecords?recordOpen.href:dashboardBack.href,document.baseURI);
      if(testMode)target.searchParams.set('testAlerts','1');
      window.history.pushState({dashboardView:view},'',target.href);
    }
    renderCrossPageAlert();
  }
  dashboardBack.addEventListener('click',event=>{event.preventDefault();setDashboardView('dashboard');});
  recordOpen.addEventListener("click",event=>{event.preventDefault();event.stopPropagation();closeMenu();setDashboardView("records");});
  window.addEventListener("popstate",()=>setDashboardView(window.location.pathname.endsWith("/Record")?"records":"dashboard",false));
  // [2026.09.17] 고친 내용: 서버가 전달한 화면 상태를 우선 적용해 조치기록 본문이 대시보드로 되돌아가지 않게 합니다.
  setDashboardView(document.body.dataset.dashboardView==="records"?"records":"dashboard",false);
  // [09.13]수정내용: 날짜 표시 설정에 현재 연도를 추가하여 대시보드에 연도와 날짜, 시간을 함께 표시한다.
  const updateClock=()=>{document.getElementById("clock").textContent=new Intl.DateTimeFormat("ko-KR",{timeZone:"Asia/Seoul",year:"numeric",month:"2-digit",day:"2-digit",weekday:"short",hour:"2-digit",minute:"2-digit",second:"2-digit",hour12:false}).format(new Date());};
  let historyDay=window.CareGuardRoomStatus.dayKey(Date.now());
  updateClock();setInterval(()=>{
    updateClock();
    const today=window.CareGuardRoomStatus.dayKey(Date.now());
    // [2026.09.16] 고친 내용: 날짜가 바뀌면 하단의 오늘 누적 카드도 함께 갱신합니다.
    if(today!==historyDay){historyDay=today;render();}
  },1000);render();
  window.addEventListener("pagehide",()=>{clearTimeout(noticeCollapseTimer);audioAllowed=false;for(const n of [...jobs.keys()])cancelAudio(n);audio.pause();});
});

