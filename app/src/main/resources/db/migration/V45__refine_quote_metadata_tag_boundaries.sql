WITH refined_descriptions(type, code, description) AS (
    VALUES
        ('EMOTION', 'EMOTION_NEW_BEGINNING', '추천 대상: 사용자가 이미 새 출발이나 전환점 앞에 서 있는 마음일 때 선택한다. 인용구가 다시 시작하게 만드는 효과나 변화 은유만으로는 제외한다.'),
        ('EMOTION', 'EMOTION_HAPPY', '추천 대상: 사용자의 현재 마음이 밝은 기쁨, 만족, 행복감에 가까울 때만 선택한다. 따뜻함, 희망, 소중함을 느끼게 하는 효과만으로는 제외한다.'),
        ('EMOTION', 'EMOTION_CALM', '추천 대상: 사용자의 현재 마음이 비교적 안정되어 조용히 사유를 이어갈 때 선택한다. 인용구의 차분한 말투나 안정감을 주는 효과만으로는 제외한다.'),
        ('EMOTION', 'EMOTION_REASSURED', '추천 대상: 걱정 뒤에 괜찮다는 확인과 안전감을 찾는 마음일 때 선택한다. 읽고 나서 안도할 수 있다는 효과만으로는 제외한다.'),
        ('EMOTION', 'EMOTION_ENERGIZED', '추천 대상: 사용자가 이미 활기를 회복하거나 움직일 힘을 느끼는 상태일 때 선택한다. 용기, 재시작, 버팀의 효과만으로는 제외한다.'),
        ('EMOTION', 'EMOTION_CONFIDENT', '추천 대상: 사용자의 현재 마음이 자기 신뢰와 확신에 가까울 때 선택한다. 인용구가 자신감을 회복시킬 수 있다는 효과만으로는 제외한다.'),
        ('EMOTION', 'EMOTION_STUCK', '추천 대상: 무엇을 해야 할지 모르거나 방향이 보이지 않는 막막함이 현재 마음의 핵심일 때 선택한다. 성찰적 문장이라도 막힘을 풀어주는 용도라면 EMOTION_THOUGHTFUL보다 우선한다.'),
        ('EMOTION', 'EMOTION_THOUGHTFUL', '추천 대상: 사용자가 이미 자기 마음, 선택, 의미를 곰곰이 돌아보는 상태일 때 선택한다. 원문이 철학적이거나 추상적이라는 이유만으로 기본 선택하지 않는다. 막막함, 불안, 후회, 공허함, 센치함이 더 분명하면 해당 감정을 우선한다.'),
        ('EMOTION', 'EMOTION_ORDINARY', '추천 대상: 특별하지 않은 일상 속에서 익숙한 흐름과 사소한 의미를 조용히 바라보는 마음일 때 선택한다. 단순 무사함이나 자극 없음은 EMOTION_UNEVENTFUL과 구분한다.'),
        ('EMOTION', 'EMOTION_UNEVENTFUL', '추천 대상: 큰 사건이나 감정 변화 없이 하루가 무난하게 지나가는 담담한 상태일 때 선택한다. 일상의 의미 발견, 깊은 성찰, 감성적 여운이 핵심이면 다른 감정을 우선한다.'),
        ('NEED', 'NEED_RESTART', '역할: 멈춘 뒤 실제로 다시 시작하거나 다음 행동으로 넘어가게 할 때 선택한다. 자기 이해, 의미 재해석, 가치 발견은 제외한다.'),
        ('NEED', 'NEED_INSPIRATION', '역할: 새로운 감각, 상상, 생각의 문을 열어줄 때 선택한다. 예쁜 표현, 희망적인 분위기, 단순 재시작 효과만으로는 제외한다.'),
        ('NEED', 'NEED_COURAGE', '역할: 두려움이나 망설임 앞에서 한 걸음을 내딛게 할 때 선택한다. 단순 희망, 위로, 관점 전환과 구분한다.'),
        ('NEED', 'NEED_MIND_CLEARING', '역할: 복잡하거나 어수선한 마음과 생각을 차분히 정돈하고 받아들이게 할 때 선택한다. 같은 상황의 의미를 새로 보게 하는 것이 핵심이면 NEED_PERSPECTIVE_SHIFT를 우선한다.'),
        ('NEED', 'NEED_PERSPECTIVE_SHIFT', '역할: 자기 자신, 관계, 상황의 의미를 다른 각도에서 재해석하게 할 때 선택한다. 단순 안정은 NEED_MIND_CLEARING, 행동으로 나아감은 NEED_COURAGE나 NEED_RESTART, 새 감각과 상상은 NEED_INSPIRATION을 우선한다.'),
        ('CONTEXT', 'CONTEXT_CLOUDY', '원문 직접 근거: 흐림, 구름, 흐린 하늘이 실제 날씨로 드러날 때만 선택한다. 흐린 마음, 어두운 기분, 희망의 대비로 쓰인 은유는 제외한다.'),
        ('CONTEXT', 'CONTEXT_SUNNY', '원문 직접 근거: 맑음, 햇살, 파란 하늘이 실제 날씨로 드러날 때만 선택한다. 빛, 밝음, 희망을 말하는 은유는 제외한다.'),
        ('CONTEXT', 'CONTEXT_SPRING', '원문 직접 근거: 봄, 꽃, 새싹이 실제 계절이나 장면으로 드러날 때만 선택한다. 성장, 시작, 회복을 말하는 은유는 제외한다.'),
        ('ROLE', 'ROLE_EMPATHY', '대표 역할: 사용자의 감정을 판단하지 않고 알아주고 인정하는 일이 중심일 때 선택한다. 해결, 조언, 통찰보다 지금 마음을 받쳐주는 의미가 강하면 우선한다.'),
        ('ROLE', 'ROLE_PERSPECTIVE', '대표 역할: 관점 전환, 의미 재해석, 본질 이해가 핵심일 때 선택한다. 철학적이거나 추상적이라는 이유로 기본 선택하지 말고, 감정 인정은 ROLE_EMPATHY, 용기와 행동 회복은 ROLE_RECOVERY를 우선 검토한다.'),
        ('ROLE', 'ROLE_RECOVERY', '대표 역할: 용기, 재시작, 변화, 행동으로 나아감이 중심일 때 선택한다. 의미를 다르게 보는 데 머무르면 ROLE_PERSPECTIVE, 감정을 알아주는 데 머무르면 ROLE_EMPATHY를 우선한다.')
)
UPDATE tags
SET description = refined_descriptions.description
FROM refined_descriptions
WHERE tags.type = refined_descriptions.type
  AND tags.code = refined_descriptions.code;
