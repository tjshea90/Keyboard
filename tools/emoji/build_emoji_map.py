import json,re,sys
# usage: python3 -I build_emoji_map.py <emojibase-data en/data.json> <output en.tsv>   (data only: never run code from the package)
d=json.load(open(sys.argv[1]))
STOP=set("the and for you are but not all can her was one our out get has him his how now see two way who did its let put say she too use new old top end back off on up down in of to is it be we he me my so do no go at as an or if by am us oh ok yes hot cut low high".split())
GENERIC=set("face with and of the in on button sign symbol mark person people man woman boy girl flag key sleeping hand body clothing animal food drink sport activity object place travel symbols small large medium dark light skin tone".split())
best={}  # word -> (prio, order, emoji)
def offer(w,prio,order,emoji):
    w=w.lower()
    if len(w)<3 or not re.fullmatch(r"[a-z]+",w) or w in STOP: return
    cur=best.get(w)
    cand=(prio,order,emoji)
    if cur is None or cand<cur: best[w]=cand
for e in d:
    if e.get('type')!=1 or 'order' not in e: continue
    if e.get('group') in (2,): pass   # component group (skin tones, hair) handled below
    emoji=e['emoji']; order=e['order']; label=e['label'].lower()
    if e.get('group')==2: continue    # components
    label=re.sub(r"[^a-z ]"," ",label)
    words=label.split()
    if len(words)==1: offer(words[0],1,order,emoji)
    else:
        continue
    if False:
        if label.replace(' ','') : pass
        for i,w in enumerate(words):
            if w in GENERIC and len(words)>1 and i>0 and w!=words[0]: continue
            if w in GENERIC: continue
            offer(w,2 if i==0 or i==len(words)-1 else 4,order,emoji)
    for t in []:
        t=t.lower()
        if ' ' in t or not re.fullmatch(r"[a-z]+",t): continue
        offer(t,3,order,emoji)
CURATED = dict(x.split(':') for x in """heart:❤️ love:❤️ car:🚗 home:🏠 house:🏠 happy:😊 smile:😊 sad:😢 sun:☀️ sunny:☀️ music:🎵 song:🎵 phone:📱 laugh:😂 lol:😂 haha:😂 thanks:🙏 thank:🙏 pray:🙏 please:🙏 cry:😭 crying:😭 kiss:😘 cool:😎 angry:😠 mad:😠 sleep:😴 sleepy:😴 tired:😴 birthday:🎂 cake:🎂 coffee:☕ food:🍔 hungry:🍔 dinner:🍽️ lunch:🥪 tea:🍵 water:💧 fire:🔥 lit:🔥 money:💰 cash:💵 wine:🍷 beer:🍺 dog:🐶 puppy:🐶 cat:🐱 kitten:🐱 bird:🐦 fish:🐟 horse:🐴 cow:🐮 pig:🐷 monkey:🐵 rain:🌧️ snow:❄️ moon:🌙 star:⭐ rainbow:🌈 flower:🌸 tree:🌳 bus:🚌 train:🚆 plane:✈️ airplane:✈️ flight:✈️ bike:🚲 ship:🚢 boat:⛵ rocket:🚀 school:🏫 hospital:🏥 book:📖 computer:💻 laptop:💻 camera:📷 photo:📷 gift:🎁 present:🎁 party:🎉 congrats:🎉 congratulations:🎉 celebrate:🎉 win:🏆 winner:🏆 trophy:🏆 idea:💡 time:⏰ clock:🕒 done:✅ check:✅ wow:😮 omg:😱 scared:😱 ghost:👻 skull:💀 dead:💀 poop:💩 devil:😈 angel:😇 baby:👶 wave:👋 hello:👋 goodbye:👋 clap:👏 strong:💪 muscle:💪 gym:🏋️ peace:✌️ nice:👍 good:👍 great:👍 awesome:🤩 amazing:🤩 perfect:👌 wink:😉 sorry:😔 sick:🤒 hot:🥵 cold:🥶 shock:😳 think:🤔 thinking:🤔 hug:🤗 hugs:🤗 clown:🤡 pizza:🍕 burger:🍔 fries:🍟 taco:🌮 sushi:🍣 apple:🍎 banana:🍌 orange:🍊 strawberry:🍓 cookie:🍪 donut:🍩 icecream:🍦 candy:🍬 chocolate:🍫 tablet:📱 mexico:🇲🇽 usa:🇺🇸 sports:⚽ soccer:⚽ football:🏈 basketball:🏀 baseball:⚾ tennis:🎾 game:🎮 games:🎮 movie:🎬 video:🎥 work:💼 job:💼 office:🏢 shopping:🛍️ shop:🛍️ sleeping:😴 dream:💭 bed:🛏️ shower:🚿 bath:🛁 toilet:🚽 key:🔑 lock:🔒 mail:✉️ email:📧 call:📞 message:💬 text:💬 chat:💬 world:🌍 earth:🌍 travel:🧳 beach:🏖️ mountain:⛰️ sunset:🌅 night:🌙 morning:🌅 sunrise:🌅 lucky:🍀 clover:🍀 magic:✨ sparkle:✨ shine:✨ hundred:💯 perfect100:💯 bomb:💣 boom:💥 explosion:💥 warning:⚠️ danger:⚠️ stop:🛑 question:❓ secret:🤫 shh:🤫 silence:🤫 quiet:🤫""".split())
for w,em in CURATED.items():
    best[w]=(0,0,em)
# plural / variants: only the singular is stored; lookup strips a trailing s
rows=sorted(best.items())
with open(sys.argv[2],'w',encoding='utf-8') as f:
    for w,(p,o,em) in rows: f.write(f"{w}\t{em}\n")
print(len(rows))
for w in ["clown","pizza","dog","heart","fire","rocket","love","happy","cat","car","money","party","pizza","beer","sad","smile","tablet","phone","star","sun","cry","laugh","lol","pray","thanks","tui","the","book","home","music"]:
    print(w, best.get(w,(0,0,'-'))[2], end=' | ')
