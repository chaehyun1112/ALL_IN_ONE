"use strict";
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

  /* [추가] 호실별 MP3 한 번 종료 후 3초 대기, 최대 5회.
     반복 타이머와 대기열은 경보별로 관리하며 한 번에 하나만 재생합니다. */
  const audio=new Audio(), jobs=new Map(), seenEvents=new Set();
  const audioControls=document.createElement("div");audioControls.className="audio-controls";
  /* [수정] 음성 켜기 체크박스 없이 재생 상태만 안내합니다. */
  audioControls.innerHTML='<span role="status" aria-live="polite">새 낙상 발생 시 음성으로 안내합니다.</span>';
  /* [수정] 음성 안내 문구는 화면에 추가하지 않습니다. 재생 기능은 유지합니다. */
  let audioAllowed=true;
  const soundInfo=audioControls.querySelector("span");
  let queue=[],playing=null,epoch=0;
  function cancelAudio(number){
    const job=jobs.get(number);if(job){job.cancelled=true;clearTimeout(job.timer);jobs.delete(number);}
    queue=queue.filter(j=>j.number!==number);
    if(playing?.number===number){epoch++;playing=null;audio.pause();audio.removeAttribute("src");audio.load();}
    pump();
  }
  async function pump(){
    if(playing||!audioAllowed)return;
    while(queue.length&&queue[0].cancelled)queue.shift();
    if(!queue.length)return;
    const job=queue.shift();playing=job;const token=++epoch;
    audio.src=new URL(`./audio/fall-${job.number}.mp3`,document.baseURI).href;
    soundInfo.textContent=`${job.number}호 음성 ${job.count+1}/5회 재생 중`;
    try{await audio.play();if(token===epoch)job.count++;}
    catch(error){
      if(token!==epoch)return;
      playing=null;
      if(error.name==="NotAllowedError"){
        queue.unshift(job);audioAllowed=false;
        soundInfo.textContent="음성을 재생하려면 화면을 한 번 클릭해주세요.";
      }else{job.cancelled=true;jobs.delete(job.number);soundInfo.textContent=`${job.number}호 음성 파일을 확인해주세요.`;pump();}
    }
  }
  audio.addEventListener("ended",()=>{
    const job=playing;playing=null;if(!job||job.cancelled)return;
    if(job.count<5){
      soundInfo.textContent=`${job.number}호 ${job.count}/5회 완료 · 3초 후 재생`;
      job.timer=setTimeout(()=>{if(!job.cancelled){queue.push(job);pump();}},3000);
    }else{jobs.delete(job.number);soundInfo.textContent=`${job.number}호 5회 안내 완료`;}
    pump();
  });
  /* [수정] 브라우저가 음성을 차단하면 다음 실제 클릭/키 입력에서 대기 경보를 재시도합니다. */
  function resumeAudio(){audioAllowed=true;pump();}
  document.addEventListener("click",resumeAudio);
  document.addEventListener("keydown",resumeAudio);

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
    detail.querySelector('.history-room').textContent=corridorSelected ? corridorAlert.location : room ? `${room.number}호` : '병실 또는 복도를 선택해 주세요';
    detail.querySelector('.history-fall').textContent=corridorSelected ? '1' : counts ? counts.urgent : '—';
    detail.querySelector('.history-exit').textContent=corridorSelected ? '0' : counts ? counts.caution : '—';
    detail.querySelector('.history-event').textContent=corridorSelected ? `낙상 감지 · ${corridorAlert.cameraLocation}` : latest ? labels[latest.type] : room ? '오늘 감지된 이벤트가 없습니다.' : '선택한 위치의 기록을 표시합니다.';
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
      /* [2026.09.17] 고친 내용: 알림 카드의 대응 등록 버튼은 해당 병실을 선택하고 등록 창을 바로 엽니다. */
      card.querySelector("button").setAttribute("aria-pressed",String(selected===room.number));
      card.querySelector("button").classList.toggle("hover-suppressed",suppressedHoverRoom===room.number);
      card.querySelector("button").addEventListener("mouseleave",event=>{
        if(suppressedHoverRoom===room.number)suppressedHoverRoom=null;
        event.currentTarget.classList.remove("hover-suppressed");
      });
      card.querySelector("button").addEventListener("click",()=>{suppressedHoverRoom=room.number;choose(room.number);openResponseRegistration();});alertPanel.append(card);
    }
    if(corridorAlert.status!=="normal"){
      const card=document.createElement("article");card.className="alert-card corridor-alert";
      // [2026.09.16] 고친 내용: 복도 알림에서는 넓은 구역명보다 실제 발생 위치를 제목으로 크게 표시합니다.
      card.innerHTML=`<div class="alert-top"><strong>● 긴급</strong></div><h3>${corridorAlert.cameraLocation} <span class="event-label">낙상 감지</span></h3><p>발생 구역 · ${corridorAlert.location}</p><button type="button" class="locate-room">대응 등록</button>`;
      card.querySelector("button").setAttribute("aria-pressed",String(corridorSelected));
      card.querySelector("button").addEventListener("click",()=>{chooseCorridor();openResponseRegistration();});alertPanel.append(card);
    }
    document.getElementById("alert-total").textContent=`${counts.urgent+counts.caution}건`;
  }

  /* [추가] 실제 SSE/WebSocket 수신부에서 아래 함수를 호출하세요.
     window.CareGuard.receiveFallEvent({ id: 서버의_고유_경보ID, room: 308 });
     동일 경보 ID 재수신은 무시합니다. 저장·통신 API는 이 파일에 포함하지 않습니다. */
  window.CareGuard={receiveFallEvent(event){
    if(!event||event.id==null||!String(event.id).trim()||!rooms.has(Number(event.room)))return false;
    const id=String(event.id);if(seenEvents.has(id)||!window.CareGuardRoomStatus.addEvent({...event,type:"urgent"}))return false;seenEvents.add(id);
    const number=Number(event.room);cancelAudio(number);
    Object.assign(rooms.get(number),{status:"urgent",acknowledged:false, eventId:id});
    {const job={number,count:0,cancelled:false};jobs.set(number,job);queue.push(job);pump();}
    render();return true;
  },receiveBedExitEvent(event){
    if(!event||!window.CareGuardRoomStatus.addEvent({...event,type:"caution"}))return false;
    const room=rooms.get(Number(event.room));
    // 같은 병실의 미해결 낙상 경보를 주의 상태로 낮추지 않습니다.
    if(room.status!=="urgent")Object.assign(room,{status:"caution",acknowledged:false,eventId:String(event.id)});
    render();return true;
  }};

  let registrationRoom=null, registrationEvent=null;
  // [2026.09.17] 고친 내용: 알림 카드에서만 대응 등록 창을 열어 범례 아래의 중복 버튼을 제거합니다.
  function openResponseRegistration(){
    const room=corridorSelected ? corridorAlert : rooms.get(selected);if(!room||room.status==="normal")return;
    registrationRoom=corridorSelected ? "corridor" : selected;registrationEvent=room.eventId;room.acknowledged=true;
    if(corridorSelected){soundInfo.textContent="중앙 복도 낙상 대응 시작";}else{cancelAudio(selected);soundInfo.textContent=`${selected}호 대응 시작 · 음성과 남은 반복 중지`;}
    form.reset();document.getElementById("response-title").textContent=`${labels[room.status]} 대응 등록`;
    const eventBox=document.getElementById("response-event");
    eventBox.textContent=`${room.location ?? `${selected}호`} · ${labels[room.status]}`;
    /* [추가] 침대 이탈 등록 창은 주의 색상 클래스를 적용합니다. */
    eventBox.classList.toggle("caution-event",room.status==="caution");
    eventBox.classList.toggle("urgent-event",room.status==="urgent");
    // [2026.09.17] 추가한 내용: 낙상 감지 대응 등록 창 전체를 긴급 테두리로 구분합니다.
    dialog.classList.toggle("urgent-response-dialog",room.status==="urgent");
    render();dialog.showModal();
  }
  document.getElementById("response-cancel").addEventListener("click",()=>dialog.close());
  /* [수정] 대응 내용을 서버에 저장할 위치입니다. 현재는 화면에만 반영됩니다. */
  form.addEventListener("submit",event=>{
    event.preventDefault();const room=registrationRoom==="corridor" ? corridorAlert : rooms.get(registrationRoom);if(!room)return;
    /* [추가] 등록 창을 연 뒤 같은 병실에 새 경보가 오면 이전 등록으로 해제하지 않습니다. */
    if(room.eventId!==registrationEvent){dialog.close();detail.textContent="새 낙상이 발생했습니다. 해당 병실의 대응 등록을 다시 열어주세요.";return;}
    const completed=new FormData(form).get("response-status")==="complete";
    // 실제 적용 시 이 위치에서 서버 저장 성공을 확인한 후 상태를 변경하세요.
    if(completed){room.status="normal";room.acknowledged=false;if(registrationRoom!=="corridor")cancelAudio(room.number);}
    dialog.close();render();
  });
  const toggle=document.getElementById("profile-toggle"),menu=document.getElementById("header-menu-list");
  /* [2026.09.17] 고친 내용: 사용자 프로필 메뉴에서 조치기록·비밀번호 변경·로그아웃을 제공합니다. */
  function closeMenu(){menu.hidden=true;toggle.setAttribute("aria-expanded","false");}
  toggle.addEventListener("click",()=>{menu.hidden=!menu.hidden;toggle.setAttribute("aria-expanded",String(!menu.hidden));});
  document.addEventListener("click",e=>{if(!e.target.closest(".header-menu"))closeMenu();});
  document.addEventListener("keydown",e=>{if(e.key==="Escape")closeMenu();});
  // [2026.09.17] 추가한 내용: 조치기록을 같은 대시보드 문서에서 열어 전체화면을 유지합니다.
  const dashboardMain=document.getElementById("dashboard-main"),recordView=document.getElementById("dashboard-record-view"),recordFrame=document.getElementById("dashboard-record-frame"),recordOpen=document.getElementById("record-open");
  function setDashboardView(view,updateAddress=true){
    const isRecords=view==="records";
    document.body.dataset.dashboardView=view;
    dashboardMain.hidden=isRecords;recordView.hidden=!isRecords;
    if(isRecords&&!recordFrame.dataset.loaded){recordFrame.src=recordFrame.dataset.src;recordFrame.dataset.loaded="true";}
    if(updateAddress)window.history.pushState({dashboardView:view},"",isRecords?"/Record":"/dashboard");
  }
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
  window.addEventListener("pagehide",()=>{audioAllowed=false;for(const n of [...jobs.keys()])cancelAudio(n);audio.pause();});
});

